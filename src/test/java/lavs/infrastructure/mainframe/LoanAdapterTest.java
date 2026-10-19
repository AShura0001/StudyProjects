package lavs.infrastructure.mainframe;

import lavs.business.model.LoanSummary;
import lavs.business.model.RepaymentEntry;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.RecordRejectedException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanAdapterTest {

    private final ScriptedConnection connection = new ScriptedConnection();
    private final LoanAdapter adapter = new LoanAdapter(connection);

    private void summaryReply(String finalPaymentDate) {
        connection.replySuccess("loanNumber", "LN-1", "customerName", "Jane Doe", "totalAmountPaid", "348000.0",
                "totalInterestPaid", "98000.0", "numberOfPayments", "360", "finalPaymentDate", finalPaymentDate,
                "finalPaymentAmount", "1449.8");
    }

    private void scheduleReply(int total, String date, int... paymentNumbers) {
        List<String> pairs = new ArrayList<>(List.of("loanNumber", "LN-1", "customerName", "Jane Doe", "page", "1",
                "total", String.valueOf(total), "items", String.valueOf(paymentNumbers.length)));
        for (int i = 0; i < paymentNumbers.length; i++) {
            String prefix = (i + 1) + ":";
            pairs.addAll(List.of(prefix + "paymentNumber", String.valueOf(paymentNumbers[i]),
                    prefix + "date", date, prefix + "amountPaid", "1450.0", prefix + "principalAmount", "400.0",
                    prefix + "interestAmount", "1050.0", prefix + "amountRemaining", "249600.0"));
        }
        connection.replySuccess(pairs.toArray(new String[0]));
    }

    @Test
    void newAdapter_withANullConnection_isRejected() {
        assertThrows(NullPointerException.class, () -> new LoanAdapter(null));
    }

    @Test
    void findSummary_withAKnownLoan_returnsTheMainframesFigures() {
        summaryReply("2056-10-01");

        Optional<LoanSummary> summary = adapter.findSummary("LN-1");

        assertEquals(Optional.of(new LoanSummary("LN-1", "Jane Doe", 360, BigDecimal.valueOf(348000.0),
                BigDecimal.valueOf(98000.0), LocalDate.of(2056, 10, 1), BigDecimal.valueOf(1449.8))), summary);
        assertEquals("loanSummary.retrieve", connection.requests().get(0).getRequestType());
        assertEquals("LN-1", connection.requests().get(0).getValue("number"));
    }

    @Test
    void findSummary_whenTheDateComesWithATime_keepsTheDay() {
        summaryReply("2056-10-01T00:00:00");

        assertEquals(LocalDate.of(2056, 10, 1), adapter.findSummary("LN-1").orElseThrow().finalPaymentDate());
    }

    @Test
    void findSummary_whenTheMainframeSaysNo_returnsEmpty() {
        connection.replyRefused("No such loan.");

        assertTrue(adapter.findSummary("LN-9").isEmpty());
    }

    @Test
    void findSummary_whenTheMainframeCannotBeReached_saysSo() {
        connection.replyUnreachable();

        MainframeUnavailableException thrown =
                assertThrows(MainframeUnavailableException.class, () -> adapter.findSummary("LN-1"));

        assertEquals(LoanAdapter.UNAVAILABLE_MESSAGE, thrown.getMessage());
    }

    @Test
    void findSummary_whenTheReplyCannotBeParsed_saysSo() {
        connection.replyUnreadable();

        assertThrows(MainframeUnavailableException.class, () -> adapter.findSummary("LN-1"));
    }

    @Test
    void findSummary_whenTheReplyIsMissingFigures_saysSoAndKeepsTheCause() {
        connection.replySuccess("loanNumber", "LN-1");

        MainframeUnavailableException thrown =
                assertThrows(MainframeUnavailableException.class, () -> adapter.findSummary("LN-1"));

        assertNotNull(thrown.getCause());
    }

    @Test
    void findSummary_whenTheDateCannotBeRead_saysSo() {
        summaryReply("not a date");

        assertThrows(MainframeUnavailableException.class, () -> adapter.findSummary("LN-1"));
    }

    @Test
    void findSummary_whenTheReplyHasNoDate_saysSo() {
        connection.replySuccess("loanNumber", "LN-1", "customerName", "Jane Doe", "totalAmountPaid", "1.0",
                "totalInterestPaid", "1.0", "numberOfPayments", "1", "finalPaymentAmount", "1.0");

        assertThrows(MainframeUnavailableException.class, () -> adapter.findSummary("LN-1"));
    }

    @Test
    void findSummary_withANullLoanNumber_isRejected() {
        assertThrows(NullPointerException.class, () -> adapter.findSummary(null));
    }

    @Test
    void findRepaymentSchedule_withOnePage_returnsTheEntriesInOrder() {
        scheduleReply(2, "2026-11-01", 1, 2);

        List<RepaymentEntry> schedule = adapter.findRepaymentSchedule("LN-1");

        assertEquals(2, schedule.size());
        RepaymentEntry first = schedule.get(0);
        assertEquals(1, first.paymentNumber());
        assertEquals(LocalDate.of(2026, 11, 1), first.date());
        assertEquals(BigDecimal.valueOf(1450.0), first.amountPaid());
        assertEquals(BigDecimal.valueOf(400.0), first.principalAmount());
        assertEquals(BigDecimal.valueOf(1050.0), first.interestAmount());
        assertEquals(BigDecimal.valueOf(249600.0), first.amountRemaining());
        assertEquals(2, schedule.get(1).paymentNumber());
        assertEquals(1, connection.requests().size());
        assertEquals("loanRepaymentSchedule.list", connection.requests().get(0).getRequestType());
        assertEquals("LN-1", connection.requests().get(0).getValue("number"));
    }

    @Test
    void findRepaymentSchedule_whenThereAreMorePagesThanOne_fetchesEveryPage() {
        scheduleReply(3, "2026-11-01", 1, 2);
        scheduleReply(3, "2026-12-01", 3);

        List<RepaymentEntry> schedule = adapter.findRepaymentSchedule("LN-1");

        assertEquals(List.of(1L, 2L, 3L), schedule.stream().map(RepaymentEntry::paymentNumber).toList());
        assertEquals(2, connection.requests().size());
        assertEquals("1", connection.requests().get(0).getValue("page"));
        assertEquals("2", connection.requests().get(1).getValue("page"));
    }

    @Test
    void findRepaymentSchedule_withNoEntries_returnsAnEmptyListAfterOneRequest() {
        scheduleReply(0, "2026-11-01");

        assertTrue(adapter.findRepaymentSchedule("LN-1").isEmpty());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void findRepaymentSchedule_whenAPageComesBackEmptyBeforeTheTotalIsReached_stopsInsteadOfLooping() {
        scheduleReply(5, "2026-11-01");

        assertTrue(adapter.findRepaymentSchedule("LN-1").isEmpty());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void findRepaymentSchedule_whenTheMainframeSaysNo_returnsAnEmptyList() {
        connection.replyRefused("No such loan.");

        assertTrue(adapter.findRepaymentSchedule("LN-9").isEmpty());
    }

    @Test
    void findRepaymentSchedule_whenTheMainframeCannotBeReached_saysSo() {
        connection.replyUnreachable();

        assertThrows(MainframeUnavailableException.class, () -> adapter.findRepaymentSchedule("LN-1"));
    }

    @Test
    void findRepaymentSchedule_whenADateCannotBeRead_saysSo() {
        scheduleReply(1, "not a date", 1);

        assertThrows(MainframeUnavailableException.class, () -> adapter.findRepaymentSchedule("LN-1"));
    }

    @Test
    void findRepaymentSchedule_whenTheReplyIsIncomplete_saysSo() {
        connection.replySuccess("loanNumber", "LN-1");

        assertThrows(MainframeUnavailableException.class, () -> adapter.findRepaymentSchedule("LN-1"));
    }

    @Test
    void findRepaymentSchedule_withANullLoanNumber_isRejected() {
        assertThrows(NullPointerException.class, () -> adapter.findRepaymentSchedule(null));
    }

    @Test
    void submitForChecking_whenTheMainframeAccepts_asksForTheStatusPending() {
        connection.replySuccess("loanNumber", "LN-1", "status", "Pending");

        adapter.submitForChecking("LN-1");

        assertEquals("loan.updateStatus", connection.requests().get(0).getRequestType());
        assertEquals("LN-1", connection.requests().get(0).getValue("loanNumber"));
        assertEquals("Pending", connection.requests().get(0).getValue("status"));
    }

    @Test
    void submitForChecking_whenTheMainframeRefuses_reportsItsReason() {
        connection.replyRefused("Loan cannot change from Active to Pending.");

        RecordRejectedException thrown =
                assertThrows(RecordRejectedException.class, () -> adapter.submitForChecking("LN-1"));

        assertEquals("Loan cannot change from Active to Pending.", thrown.getMessage());
    }

    @Test
    void submitForChecking_whenTheMainframeCannotBeReached_saysSo() {
        connection.replyUnreachable();

        assertThrows(MainframeUnavailableException.class, () -> adapter.submitForChecking("LN-1"));
    }

    @Test
    void submitForChecking_withANullLoanNumber_isRejected() {
        assertThrows(NullPointerException.class, () -> adapter.submitForChecking(null));
    }
}
