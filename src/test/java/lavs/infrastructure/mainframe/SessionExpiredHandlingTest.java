package lavs.infrastructure.mainframe;

import lavs.business.model.CustomerReference;
import lavs.business.model.Draft;
import lavs.business.model.DraftType;
import lavs.business.model.LoanDetails;
import lavs.business.model.LoanPayloads;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.SessionExpiredException;
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** An ended session is reported as such, once, and never leaves a draft "uncertain". */
class SessionExpiredHandlingTest {

    /** Answers every request with an ended session, or success after login. */
    private static final class EndedSession implements Connection {
        boolean accepting;
        int logins;

        @Override
        public MessageResponse send(MessageRequest request) {
            return accepting
                    ? new MessageResponse(new Status(1), new HashMap<>())
                    : StatusMessage.SESSION_EXPIRED.generateMessageResponse(1);
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public Status Login(String username, String password) {
            logins++;
            return new Status(1);
        }

        @Override
        public void Logout() {
        }

        @Override
        public void close() {
        }
    }

    @Test
    void theWatcherReportsAnEndedSessionOnce_andAgainAfterTheNextLogin() throws Exception {
        EndedSession inner = new EndedSession();
        SessionWatchingConnection connection = new SessionWatchingConnection(inner);
        AtomicInteger reported = new AtomicInteger();
        connection.onSessionLost(reported::incrementAndGet);

        connection.send(new MessageRequest(MessageRequest.QUERY, "customer.search"));
        connection.send(new MessageRequest(MessageRequest.QUERY, "customer.search"));
        assertEquals(1, reported.get(), "many failing requests, one report");

        connection.Login("tester", "pw");
        connection.send(new MessageRequest(MessageRequest.QUERY, "customer.search"));
        assertEquals(2, reported.get(), "re-armed by the new login");
        assertEquals(1, inner.logins);
    }

    @Test
    void theWatcherStaysQuietWhileTheSessionIsAccepted() {
        EndedSession inner = new EndedSession();
        inner.accepting = true;
        SessionWatchingConnection connection = new SessionWatchingConnection(inner);
        AtomicInteger reported = new AtomicInteger();
        connection.onSessionLost(reported::incrementAndGet);

        connection.send(new MessageRequest(MessageRequest.QUERY, "customer.search"));

        assertEquals(0, reported.get());
    }

    @Test
    void adaptersReportAnEndedSession_asAKindOfUnavailable() {
        LoanAdapter loans = new LoanAdapter(new EndedSession());

        SessionExpiredException ended = assertThrows(SessionExpiredException.class, () -> loans.findLoan("LN-1"));
        assertTrue(ended instanceof MainframeUnavailableException);
        assertEquals(SessionExpiredException.MESSAGE, ended.getMessage());
    }

    @Test
    void aDraftTurnedAwayByAnEndedSession_isSafelyUnsent_notUncertain() {
        LoanDetails details = new LoanDetails(new BigDecimal("1000"), "Fixed", new BigDecimal("5"), "Yearly",
                new BigDecimal("100"), "Monthly", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));
        Draft loan = new Draft(DraftType.LOAN, LoanPayloads.loanPayload(CustomerReference.existing("CUS-1"), details));

        assertThrows(SessionExpiredException.class,
                () -> new MainframeDraftUploader(new EndedSession()).upload(loan, List.of()),
                "SessionExpiredException is a MainframeUnavailableException, so the sync puts the draft back to waiting");
    }
}
