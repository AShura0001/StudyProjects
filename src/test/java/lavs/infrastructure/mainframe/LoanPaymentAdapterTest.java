package lavs.infrastructure.mainframe;

import lavs.business.model.LoanPayment;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.PageResult;
import lavs.business.repository.RecordRejectedException;
import lavs.business.repository.SessionExpiredException;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The loan payment adapter against a scripted connection, including how failures are reported. */
class LoanPaymentAdapterTest {

    private final ScriptedConnection connection = new ScriptedConnection();
    private final LoanPaymentAdapter adapter = new LoanPaymentAdapter(connection);

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void findPayment_readsTheDateAndEveryAmount() {
        connection.replySuccess("loanNumber", "LN-1", "customerName", "Jane Doe", "id", "4",
                "date", "2026-01-15T00:00:00", "amountPaid", "1450.0", "principalAmount", "400.0",
                "interestAmount", "1050.0", "amountRemaining", "249600.0");

        LoanPayment payment = adapter.findPayment("LN-1", 4).orElseThrow();

        assertEquals(4, payment.id());
        assertEquals(LocalDate.of(2026, 1, 15), payment.date());
        assertMoney("1450", payment.amountPaid());
        assertMoney("400", payment.principalAmount());
        assertMoney("1050", payment.interestAmount());
        assertMoney("249600", payment.amountRemaining());
        MessageRequest sent = connection.requests().get(0);
        assertEquals("loanPayment.retrieve", sent.getRequestType());
        assertEquals("4", sent.getValue("id"));
        assertEquals("LN-1", sent.getValue("number"));
    }

    @Test
    void findPayment_whenTheReplyLeavesOutTheLoanNumber_usesTheOneAskedFor() {
        connection.replySuccess("id", "4", "date", "2026-01-15", "amountPaid", "1450.0", "principalAmount", "400.0",
                "interestAmount", "1050.0", "amountRemaining", "249600.0");

        LoanPayment payment = adapter.findPayment("LN-1", 4).orElseThrow();

        assertEquals("LN-1", payment.loanNumber());
    }

    @Test
    void findPayment_whenTheReplyLeavesOutAnAmount_saysTheReplyCouldNotBeRead() {
        connection.replySuccess("loanNumber", "LN-1", "id", "4", "date", "2026-01-15", "amountPaid", "1450.0");

        MainframeUnavailableException unreadable = assertThrows(MainframeUnavailableException.class,
                () -> adapter.findPayment("LN-1", 4));
        assertEquals(MainframeCalls.UNREADABLE_MESSAGE, unreadable.getMessage());
    }

    @Test
    void findPayment_whenThereIsNone_isEmpty() {
        connection.replyRefused("No such payment.");

        assertTrue(adapter.findPayment("LN-1", 4).isEmpty());
    }

    @Test
    void findPayment_withoutADate_saysWhatTheMainframeLeftOut() {
        connection.replySuccess("loanNumber", "LN-1", "id", "4", "amountPaid", "1450.0", "principalAmount", "400.0",
                "interestAmount", "1050.0", "amountRemaining", "249600.0");

        IllegalStateException missing = assertThrows(IllegalStateException.class, () -> adapter.findPayment("LN-1", 4));
        assertTrue(missing.getMessage().contains("payment date"));
    }

    @Test
    void findPayment_whenTheMainframeCannotBeReached_saysSo() {
        connection.replyUnreachable();

        MainframeUnavailableException offline = assertThrows(MainframeUnavailableException.class,
                () -> adapter.findPayment("LN-1", 4));
        assertEquals(MainframeCalls.UNREACHABLE_MESSAGE, offline.getMessage());
    }

    @Test
    void findPayment_whenTheSessionHasEnded_saysSo() {
        connection.replySessionExpired();

        assertThrows(SessionExpiredException.class, () -> adapter.findPayment("LN-1", 4));
    }

    @Test
    void listPayments_readsEachPaymentAndTheTotal() {
        connection.replySuccess("loanNumber", "LN-1", "customerName", "Jane Doe", "page", "1", "total", "2",
                "items", "2",
                "1:id", "4", "1:date", "2026-01-15", "1:amountPaid", "1450.0",
                "2:id", "5", "2:date", "2026-02-15", "2:amountPaid", "1450.0");

        PageResult<LoanPayment> page = adapter.listPayments("LN-1", 1, 100);

        assertEquals(2, page.getTotal());
        assertEquals(LocalDate.of(2026, 2, 15), page.getList().get(1).date());
        assertEquals("LN-1", page.getList().get(1).loanNumber());
        assertEquals("loanPayment.list", connection.requests().get(0).getRequestType());
    }

    @Test
    void listPayments_whenTheReplyLeavesOutTheLoanNumber_usesTheOneAskedFor() {
        connection.replySuccess("page", "1", "total", "1", "items", "1",
                "1:id", "4", "1:date", "2026-01-15", "1:amountPaid", "1450.0");

        assertEquals("LN-1", adapter.listPayments("LN-1", 1, 100).getList().get(0).loanNumber());
    }

    @Test
    void listPayments_whenTheMainframeRefuses_isAnEmptyPage() {
        connection.replyRefused("No such loan.");

        PageResult<LoanPayment> page = adapter.listPayments("LN-404", 1, 100);

        assertTrue(page.getList().isEmpty());
    }

    @Test
    void addPayment_sendsItAndReadsTheSplitTheMainframeWorkedOut() {
        connection.replySuccess("loanNumber", "LN-1", "id", "6", "date", "2026-03-15", "amountPaid", "1450.0",
                "principalAmount", "410.0", "interestAmount", "1040.0", "amountRemaining", "249190.0");

        LoanPayment added = adapter.addPayment("LN-1", LocalDate.of(2026, 3, 15), new BigDecimal("1450.00"));

        assertEquals(6, added.id());
        assertMoney("410", added.principalAmount());
        assertMoney("249190", added.amountRemaining());
        MessageRequest sent = connection.requests().get(0);
        assertEquals("loan.addPayment", sent.getRequestType());
        assertEquals("2026-03-15", sent.getValue("date"));
        assertEquals("LN-1", sent.getValue("loanNumber"));
    }

    @Test
    void addPayment_whenTheMainframeRefuses_passesItsReasonOn() {
        connection.replyRefused("The loan is not active.");

        RecordRejectedException refused = assertThrows(RecordRejectedException.class,
                () -> adapter.addPayment("LN-1", LocalDate.of(2026, 3, 15), new BigDecimal("1450")));
        assertEquals("The loan is not active.", refused.getMessage());
    }

    @Test
    void addPayment_whenTheMainframeRefusesWithoutAReason_saysItWasRefused() {
        connection.replyRefused("  ");

        RecordRejectedException refused = assertThrows(RecordRejectedException.class,
                () -> adapter.addPayment("LN-1", LocalDate.of(2026, 3, 15), new BigDecimal("1450")));
        assertEquals(MainframeCalls.REJECTED_MESSAGE, refused.getMessage());
    }

    @Test
    void deletePayment_sendsTheRemoval_andReportsARefusal() {
        connection.replySuccess("loanNumber", "LN-1", "id", "6");
        connection.replyRefused("No such payment.");

        adapter.deletePayment("LN-1", 6);
        assertEquals("loan.deletePayment", connection.requests().get(0).getRequestType());

        assertThrows(RecordRejectedException.class, () -> adapter.deletePayment("LN-1", 6));
    }
}
