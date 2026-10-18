package lavs.app;

import lavs.app.AppServices.Gateways;
import lavs.business.model.CustomerProfile;
import lavs.business.model.CustomerReference;
import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;
import lavs.business.model.DraftType;
import lavs.business.model.LoanDetails;
import lavs.business.model.LoanPayloads;
import lavs.business.repository.CustomerGateway;
import lavs.business.repository.CustomerRelationshipGateway;
import lavs.business.repository.FakeDraftUploadGateway;
import lavs.business.repository.LoanGateway;
import lavs.business.service.BasicLoanValidator;
import lavs.business.service.CustomerProfileSource;
import lavs.business.service.DraftSyncService.SyncResult;
import lavs.business.service.ManualScheduler;
import lavs.business.service.RetryPolicy;
import lavs.business.service.SyncListener;
import lavs.data.JsonDraftRepository;
import lavs.infrastructure.mainframe.ScriptedConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppServicesTest {

    private static final LoanDetails DETAILS = new LoanDetails(new BigDecimal("250000.00"), "Fixed",
            new BigDecimal("5.85"), "Yearly", new BigDecimal("1450.00"), "Monthly",
            LocalDate.of(2026, 10, 1), LocalDate.of(2031, 10, 1));
    private static final CustomerReference CUSTOMER = CustomerReference.existing("01-0001-0000020");

    @TempDir
    Path folder;

    private final FakeDraftUploadGateway mainframe = new FakeDraftUploadGateway();
    private final ManualScheduler scheduler = new ManualScheduler();
    private final CustomerProfileSource profiles = mock(CustomerProfileSource.class);
    private final List<SyncResult> finished = new ArrayList<>();
    private final SyncListener listener = new SyncListener() {
        @Override
        public void onSyncFinished(SyncResult result) {
            finished.add(result);
        }
    };

    private Gateways gateways() {
        return new Gateways(mainframe, mock(LoanGateway.class), mock(CustomerGateway.class),
                mock(CustomerRelationshipGateway.class));
    }

    private AppServices start() {
        when(profiles.profileOf(any())).thenReturn(Optional.of(new CustomerProfile(true, true, true)));
        return AppServices.start(folder, gateways(), profiles, new BasicLoanValidator(), listener,
                scheduler, RetryPolicy.standard());
    }

    private Draft storedDraft(String id) {
        return new JsonDraftRepository(folder).findById(id).orElseThrow();
    }

    @Test
    void start_withEverythingNeeded_givesWorkingServices() {
        try (AppServices app = start()) {
            Draft loan = app.loans().createLoan(CUSTOMER, DETAILS);
            scheduler.runImmediate();

            assertTrue(Files.exists(folder.resolve(loan.getLocalId() + ".json")));
            assertEquals(DraftStatus.SYNCED, storedDraft(loan.getLocalId()).getStatus());
            assertEquals(1, mainframe.recordsCreated());
        }
    }

    @Test
    void start_atOnce_asksForAPassSoDraftsLeftFromLastTimeAreSent() {
        Draft leftOver = new Draft(DraftType.LOAN, LoanPayloads.loanPayload(CUSTOMER, DETAILS));
        new JsonDraftRepository(folder).save(leftOver);

        try (AppServices app = start()) {
            assertEquals(1, scheduler.immediateCount());
            scheduler.runImmediate();

            assertEquals(DraftStatus.SYNCED, storedDraft(leftOver.getLocalId()).getStatus());
            assertEquals(List.of(new SyncResult(1, 0, 0, 0, false)), finished);
        }
    }

    @Test
    void aLoanSavedOfflineIsSentExactlyOnceAfterTheApplicationRestarts() {
        mainframe.goOffline();
        Draft loan;
        try (AppServices first = start()) {
            loan = first.loans().createLoan(CUSTOMER, DETAILS);
            scheduler.runImmediate();
            assertEquals(DraftStatus.PENDING, storedDraft(loan.getLocalId()).getStatus());
        }

        mainframe.goOnline();
        ManualScheduler afterRestart = new ManualScheduler();
        when(profiles.profileOf(any())).thenReturn(Optional.of(new CustomerProfile(true, true, true)));
        try (AppServices second = AppServices.start(folder, gateways(), profiles, new BasicLoanValidator(),
                listener, afterRestart, RetryPolicy.standard())) {
            afterRestart.runImmediate();

            assertEquals(DraftStatus.SYNCED, storedDraft(loan.getLocalId()).getStatus());
            assertEquals(1, mainframe.recordsCreated());
        }
    }

    @Test
    void anUploadCutShortByTheApplicationStopping_isMarkedUncertainAndNotSentAgain() {
        Draft interrupted = new Draft(interruptedId(), DraftType.LOAN, LoanPayloads.loanPayload(CUSTOMER, DETAILS),
                DraftStatus.SENDING, Instant.parse("2026-10-01T09:00:00Z"), null, Map.of());
        new JsonDraftRepository(folder).save(interrupted);

        try (AppServices app = start()) {
            scheduler.runImmediate();

            assertEquals(DraftStatus.UNCERTAIN, storedDraft(interruptedId()).getStatus());
            assertEquals(1, app.sync().findUncertain().size());
            assertTrue(mainframe.attempts().isEmpty());
        }
    }

    private static String interruptedId() {
        return "interrupted-upload";
    }

    @Test
    void close_stopsTheBackgroundWorkAndIgnoresLaterRequests() {
        AppServices app = start();
        scheduler.runImmediate();

        app.close();
        app.sync().requestSync();

        assertTrue(scheduler.isShutDown());
        assertEquals(0, scheduler.immediateCount());
    }

    @Test
    void close_keepsDraftsThatWereAlreadySaved() {
        mainframe.goOffline();
        Draft loan;
        try (AppServices app = start()) {
            loan = app.loans().createLoan(CUSTOMER, DETAILS);
        }

        assertEquals(DraftStatus.PENDING, storedDraft(loan.getLocalId()).getStatus());
    }

    @Test
    void relationshipsAndLoansAreAvailable() {
        try (AppServices app = start()) {
            assertFalse(app.loans() == null);
            assertFalse(app.relationships() == null);
            assertFalse(app.sync() == null);
        }
    }

    @Test
    void defaultDraftsFolder_isInsideTheUsersHomeFolder() {
        Path expected = Path.of(System.getProperty("user.home"), ".lavs", "drafts");

        assertEquals(expected, AppServices.defaultDraftsFolder());
    }

    @Test
    void start_withANullArgument_isRejected() {
        Gateways gateways = gateways();
        BasicLoanValidator validator = new BasicLoanValidator();
        RetryPolicy policy = RetryPolicy.standard();
        assertThrows(NullPointerException.class,
                () -> AppServices.start(null, gateways, profiles, validator, listener, scheduler, policy));
        assertThrows(NullPointerException.class,
                () -> AppServices.start(folder, null, profiles, validator, listener, scheduler, policy));
        assertThrows(NullPointerException.class,
                () -> AppServices.start(folder, gateways, null, validator, listener, scheduler, policy));
        assertThrows(NullPointerException.class,
                () -> AppServices.start(folder, gateways, profiles, null, listener, scheduler, policy));
        assertThrows(NullPointerException.class,
                () -> AppServices.start(folder, gateways, profiles, validator, null, scheduler, policy));
        assertThrows(NullPointerException.class,
                () -> AppServices.start(folder, gateways, profiles, validator, listener, null, policy));
        assertThrows(NullPointerException.class,
                () -> AppServices.start(folder, gateways, profiles, validator, listener, scheduler, null));
    }

    private static void loanReply(ScriptedConnection connection, String loanNumber) {
        connection.replySuccess("loanNumber", loanNumber, "customerNumber", "01-0001-0000020", "rateType", "Fixed",
                "rateAmount", "5.85", "rateFrequency", "Yearly", "paymentAmount", "1450.0",
                "paymentFrequency", "Monthly", "principal", "250000.0", "startDate", "2026-10-01",
                "renewalDate", "2031-10-01", "status", "New");
    }

    @Test
    void forMainframe_aLoanReachesTheMainframeThroughTheRealAdapters() throws InterruptedException {
        when(profiles.profileOf(any())).thenReturn(Optional.of(new CustomerProfile(true, true, true)));
        ScriptedConnection connection = new ScriptedConnection();
        loanReply(connection, "LN-55");
        CountDownLatch arrived = new CountDownLatch(1);
        SyncListener watching = new SyncListener() {
            @Override
            public void onSyncFinished(SyncResult result) {
                if (result.synced() > 0) {
                    arrived.countDown();
                }
            }
        };
        try (AppServices app = AppServices.forMainframe(connection, folder, profiles, new BasicLoanValidator(),
                watching)) {
            Draft loan = app.loans().createLoan(CUSTOMER, DETAILS);

            assertTrue(arrived.await(5, TimeUnit.SECONDS));

            assertEquals("LN-55", storedDraft(loan.getLocalId()).getServerNumber().orElseThrow());
            assertEquals(1, connection.requests().size());
            assertEquals("loan.create", connection.requests().get(0).getRequestType());
            assertTrue(connection.pings() >= 1);
        }
    }

    @Test
    void forMainframe_aLoanSavedWhileTheMainframeIsUnreachableIsSentOnceWhenItReturns() throws InterruptedException {
        when(profiles.profileOf(any())).thenReturn(Optional.of(new CustomerProfile(true, true, true)));
        ScriptedConnection connection = new ScriptedConnection();
        connection.setConnected(false);
        CountDownLatch lost = new CountDownLatch(1);
        CountDownLatch arrived = new CountDownLatch(1);
        SyncListener watching = new SyncListener() {
            @Override
            public void onConnectionLost(SyncResult result, java.time.Duration retryIn) {
                lost.countDown();
            }

            @Override
            public void onSyncFinished(SyncResult result) {
                if (result.synced() > 0) {
                    arrived.countDown();
                }
            }
        };
        try (AppServices app = AppServices.forMainframe(connection, folder, profiles, new BasicLoanValidator(),
                watching)) {
            Draft loan = app.loans().createLoan(CUSTOMER, DETAILS);

            assertTrue(lost.await(5, TimeUnit.SECONDS));
            assertEquals(DraftStatus.PENDING, storedDraft(loan.getLocalId()).getStatus());
            assertTrue(connection.requests().isEmpty());
            loanReply(connection, "LN-56");
            connection.setConnected(true);
            assertTrue(arrived.await(10, TimeUnit.SECONDS));

            assertEquals(DraftStatus.SYNCED, storedDraft(loan.getLocalId()).getStatus());
            assertEquals(1, connection.requests().size());
        }
    }

    @Test
    void forMainframe_withANullConnection_isRejected() {
        assertThrows(NullPointerException.class, () -> AppServices.forMainframe(null, folder, profiles,
                new BasicLoanValidator(), listener));
        assertThrows(NullPointerException.class, () -> AppServices.mainframeGateways(null));
    }

    @Test
    void gateways_withANullGateway_isRejected() {
        assertThrows(NullPointerException.class, () -> new Gateways(null, mock(LoanGateway.class),
                mock(CustomerGateway.class), mock(CustomerRelationshipGateway.class)));
        assertThrows(NullPointerException.class, () -> new Gateways(mainframe, null,
                mock(CustomerGateway.class), mock(CustomerRelationshipGateway.class)));
        assertThrows(NullPointerException.class, () -> new Gateways(mainframe, mock(LoanGateway.class),
                null, mock(CustomerRelationshipGateway.class)));
        assertThrows(NullPointerException.class, () -> new Gateways(mainframe, mock(LoanGateway.class),
                mock(CustomerGateway.class), null));
    }

    @Test
    void withARealWorkerThread_aLoanSavedWhileOfflineArrivesByItselfWhenTheConnectionReturns()
            throws InterruptedException {
        when(profiles.profileOf(any())).thenReturn(Optional.of(new CustomerProfile(true, true, true)));
        CountDownLatch lost = new CountDownLatch(1);
        CountDownLatch arrived = new CountDownLatch(1);
        List<SyncResult> seen = new CopyOnWriteArrayList<>();
        SyncListener watching = new SyncListener() {
            @Override
            public void onConnectionLost(SyncResult result, java.time.Duration retryIn) {
                lost.countDown();
            }

            @Override
            public void onSyncFinished(SyncResult result) {
                seen.add(result);
                if (result.synced() > 0) {
                    arrived.countDown();
                }
            }
        };
        mainframe.goOffline();
        try (AppServices app = AppServices.start(folder, gateways(), profiles, new BasicLoanValidator(), watching,
                new lavs.infrastructure.concurrent.ExecutorScheduler(),
                new RetryPolicy(java.time.Duration.ofMillis(10), java.time.Duration.ofMillis(40), () -> 1.0))) {
            Draft loan = app.loans().createLoan(CUSTOMER, DETAILS);

            assertTrue(lost.await(5, TimeUnit.SECONDS));
            assertEquals(DraftStatus.PENDING, storedDraft(loan.getLocalId()).getStatus());
            mainframe.goOnline();
            assertTrue(arrived.await(5, TimeUnit.SECONDS));

            assertEquals(DraftStatus.SYNCED, storedDraft(loan.getLocalId()).getStatus());
            assertEquals(1, mainframe.recordsCreated());
        }
    }
}
