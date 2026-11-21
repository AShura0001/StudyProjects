package lavs.business.service;

import lavs.app.AppServices;
import lavs.business.model.CustomerReference;
import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;
import lavs.business.model.DraftType;
import lavs.business.model.LoanDetails;
import lavs.business.model.LoanPayloads;
import lavs.business.repository.FakeDraftRepository;
import lavs.business.repository.FakeDraftUploadGateway;
import lavs.business.repository.LoanGateway;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Fixing a refused loan, listing a customer's unsent loans, and where drafts are kept. */
class LoanServiceUnsentDraftsTest {

    private static final LoanDetails DETAILS = new LoanDetails(new BigDecimal("250000"), "Fixed",
            new BigDecimal("5.85"), "Yearly", new BigDecimal("1450"), "Monthly",
            LocalDate.of(2026, 1, 1), LocalDate.of(2031, 1, 1));

    private final FakeDraftRepository repository = new FakeDraftRepository();
    private final LoanService service = new LoanService(mock(LoanGateway.class), mock(CustomerProfileSource.class),
            new BasicLoanValidator(),
            new BackgroundSync(new DraftSyncService(repository, new FakeDraftUploadGateway()),
                    new ManualScheduler(), RetryPolicy.standard()),
            repository);

    private Draft loanFor(String id, String customer, DraftStatus status) {
        Draft draft = new Draft(id, DraftType.LOAN,
                LoanPayloads.loanPayload(CustomerReference.existing(customer), DETAILS), status, Instant.now(),
                status == DraftStatus.SYNCED ? "LN-" + id : null,
                status == DraftStatus.REJECTED ? Map.of("general", "rate too high") : Map.of(), List.of());
        repository.save(draft);
        return draft;
    }

    @Test
    void reviseLoan_checksTheRules_thenQueuesTheCorrectedLoan() {
        loanFor("d1", "CUS-1", DraftStatus.REJECTED);
        LoanDetails broken = new LoanDetails(BigDecimal.ZERO, "Fixed", new BigDecimal("5.85"), "Yearly",
                new BigDecimal("1450"), "Monthly", LocalDate.of(2026, 1, 1), LocalDate.of(2031, 1, 1));
        assertThrows(LoanRuleException.class, () -> service.reviseLoan("d1", broken));
        assertEquals(DraftStatus.REJECTED, repository.findById("d1").orElseThrow().getStatus());

        LoanDetails fixed = new LoanDetails(new BigDecimal("250000"), "Fixed", new BigDecimal("4.95"), "Yearly",
                new BigDecimal("1450"), "Monthly", LocalDate.of(2026, 1, 1), LocalDate.of(2031, 1, 1));
        Draft revised = service.reviseLoan("d1", fixed);

        assertEquals(DraftStatus.PENDING, revised.getStatus());
        LoanPayloads.LoanContent content = LoanPayloads.readLoan(revised.getPayload());
        assertEquals(CustomerReference.existing("CUS-1"), content.customer(), "the customer is kept");
        assertEquals(0, new BigDecimal("4.95").compareTo(content.details().rateAmount()));
    }

    @Test
    void unsentLoansOf_listsOnlyThatCustomersLoansNotYetConfirmed() {
        loanFor("a", "CUS-1", DraftStatus.PENDING);
        loanFor("b", "CUS-1", DraftStatus.REJECTED);
        loanFor("c", "CUS-1", DraftStatus.SYNCED);
        loanFor("d", "CUS-2", DraftStatus.PENDING);

        assertEquals(List.of("a", "b"),
                service.unsentLoansOf("CUS-1").stream().map(Draft::getLocalId).sorted().toList());
    }

    @Test
    void eachEmployeeHasTheirOwnDraftsFolder() {
        Path jane = AppServices.draftsFolderFor("JDoe");
        Path sam = AppServices.draftsFolderFor("sam.lee");
        assertNotEquals(jane, sam);
        assertEquals(AppServices.defaultDraftsFolder(), jane.getParent());
        assertEquals("jdoe", jane.getFileName().toString());
        assertEquals("a_b", AppServices.draftsFolderFor("a/b").getFileName().toString(), "no path tricks");
        assertThrows(IllegalArgumentException.class, () -> AppServices.draftsFolderFor(" "));
        assertThrows(IllegalArgumentException.class, () -> AppServices.draftsFolderFor(".."));
    }
}
