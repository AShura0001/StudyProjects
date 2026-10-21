package lavs.infrastructure.mainframe;

import lavs.business.model.LoanDetails;
import lavs.business.model.LoanListing;
import lavs.business.model.LoanRecord;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.RecordRejectedException;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** The loan list, retrieve and update calls the loan screens use. */
class LoanAdapterRecordsTest {

    private final ScriptedConnection connection = new ScriptedConnection();
    private final LoanAdapter adapter = new LoanAdapter(connection);

    @Test
    void listLoans_readsEachLoan() {
        connection.replySuccess("customerNumber", "CUS-1", "page", "1", "total", "2", "items", "2",
                "1:loanNumber", "LN-1", "1:principal", "250000.0", "1:startDate", "2025-01-01T00:00:00", "1:status", "Active",
                "2:loanNumber", "LN-2", "2:principal", "", "2:startDate", "", "2:status", "New");

        List<LoanListing> loans = adapter.listLoans("CUS-1");

        assertEquals(List.of(
                new LoanListing("LN-1", new BigDecimal("250000.0"), LocalDate.of(2025, 1, 1), "Active"),
                new LoanListing("LN-2", null, null, "New")), loans);
        assertEquals("loan.list", connection.requests().get(0).getRequestType());
    }

    @Test
    void findLoan_readsTheTerms() {
        connection.replySuccess("loanNumber", "LN-1", "customerNumber", "CUS-1", "customerName", "Jane Doe",
                "status", "Active", "principal", "250000.0", "rateType", "Fixed", "rateAmount", "5.85",
                "rateFrequency", "Yearly", "paymentAmount", "1450.0", "paymentFrequency", "Monthly",
                "startDate", "2025-01-01", "renewalDate", "2030-01-01");

        LoanRecord loan = adapter.findLoan("LN-1").orElseThrow();

        assertEquals("CUS-1", loan.customerNumber());
        assertEquals(0, new BigDecimal("5.85").compareTo(loan.rateAmount()));
        assertEquals(LocalDate.of(2030, 1, 1), loan.renewalDate());
        assertEquals("loan.retrieve", connection.requests().get(0).getRequestType());
    }

    @Test
    void findLoan_ofAnUnknownLoan_isEmpty() {
        connection.replyRefused("no such loan");
        assertEquals(Optional.empty(), adapter.findLoan("LN-404"));
    }

    private void updateReply() {
        connection.replySuccess("loanNumber", "LN-1", "customerNumber", "CUS-1", "rateType", "Fixed",
                "rateAmount", "5.85", "rateFrequency", "Yearly", "paymentAmount", "1600.0",
                "paymentFrequency", "Fortnightly", "principal", "250000.0", "startDate", "2025-01-01",
                "renewalDate", "2030-01-01", "status", "Active");
    }

    @Test
    void updateLoan_sendsEveryTerm() {
        updateReply();
        LoanDetails terms = new LoanDetails(new BigDecimal("250000"), "Fixed", new BigDecimal("5.85"), "Yearly",
                new BigDecimal("1600"), "Fortnightly", LocalDate.of(2025, 1, 1), LocalDate.of(2030, 1, 1));

        adapter.updateLoan("LN-1", "CUS-1", terms);

        MessageRequest sent = connection.requests().get(0);
        assertEquals("loan.update", sent.getRequestType());
        assertEquals("Fortnightly", sent.getValue("paymentFrequency"));
        assertEquals("2030-01-01", sent.getValue("renewalDate"));
    }

    @Test
    void updateLoan_refusedOrUnreachable_isReported() {
        LoanDetails terms = new LoanDetails(new BigDecimal("1"), "Fixed", new BigDecimal("1"), "Yearly",
                new BigDecimal("1"), "Monthly", LocalDate.of(2025, 1, 1), LocalDate.of(2030, 1, 1));
        connection.replyRefused("loan is closed");
        assertThrows(RecordRejectedException.class, () -> adapter.updateLoan("LN-1", "CUS-1", terms));
        connection.replyUnreachable();
        assertThrows(MainframeUnavailableException.class, () -> adapter.updateLoan("LN-1", "CUS-1", terms));
    }
}
