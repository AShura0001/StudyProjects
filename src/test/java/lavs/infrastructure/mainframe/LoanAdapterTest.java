package lavs.infrastructure.mainframe;

import lavs.business.model.LoanSummary;
import lavs.business.model.RepaymentEntry;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.RecordRejectedException;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static lavs.infrastructure.mainframe.MainframeTestConnection.failedResponse;
import static lavs.infrastructure.mainframe.MainframeTestConnection.successResponse;
import static org.junit.jupiter.api.Assertions.*;

class LoanAdapterTest {

    private static final String LOAN = "01-0001-0000020-001";

    private static MessageResponse networkFailure() {
        return StatusMessage.NETWORK_FAILURE_UNAVAILABLE.generateMessageResponse(1);
    }

    @Test
    void findSummary_whenMainframeSucceeds_mapsSummary() {
        MainframeTestConnection connection = new MainframeTestConnection();
        connection.addResponse(successResponse(
                "loanNumber", LOAN,
                "customerName", "Jane Smith",
                "numberOfPayments", "60",
                "totalAmountPaid", "1234.5",
                "totalInterestPaid", "234.5",
                "finalPaymentDate", "2030-01-02",
                "finalPaymentAmount", "20.25"));

        Optional<LoanSummary> summary = new LoanAdapter(connection).findSummary(LOAN);

        LoanSummary result = summary.orElseThrow();
        assertEquals(LOAN, result.loanNumber());
        assertEquals("Jane Smith", result.customerName());
        assertEquals(60, result.numberOfPayments());
        assertEquals(new BigDecimal("1234.5"), result.totalAmountPaid());
        assertEquals(new BigDecimal("234.5"), result.totalInterestPaid());
        assertEquals(LocalDate.of(2030, 1, 2), result.finalPaymentDate());
        assertEquals(new BigDecimal("20.25"), result.finalPaymentAmount());
        assertEquals("loanSummary.retrieve", connection.getLastRequest().getRequestType());
        assertEquals(LOAN, connection.getLastRequest().getValue("number"));
    }

    @Test
    void findSummary_whenMainframeRefuses_isEmpty() {
        MainframeTestConnection connection = new MainframeTestConnection();
        connection.addResponse(failedResponse());

        assertTrue(new LoanAdapter(connection).findSummary(LOAN).isEmpty());
    }

    @Test
    void findSummary_whenMainframeUnreachable_throws() {
        MainframeTestConnection connection = new MainframeTestConnection();
        connection.addResponse(networkFailure());

        assertThrows(MainframeUnavailableException.class, () -> new LoanAdapter(connection).findSummary(LOAN));
    }

    @Test
    void findRepaymentSchedule_fetchesEveryPage() {
        MainframeTestConnection connection = new MainframeTestConnection();
        connection.addResponse(schedulePage(1, 3, 1, 2));
        connection.addResponse(schedulePage(2, 3, 3));

        List<RepaymentEntry> schedule = new LoanAdapter(connection).findRepaymentSchedule(LOAN);

        assertEquals(List.of(1L, 2L, 3L), schedule.stream().map(RepaymentEntry::paymentNumber).toList());
        assertEquals(LocalDate.of(2026, 1, 1), schedule.get(0).date());
        assertEquals(new BigDecimal("100.0"), schedule.get(0).amountPaid());
        assertEquals(new BigDecimal("90.0"), schedule.get(0).principalAmount());
        assertEquals(new BigDecimal("10.0"), schedule.get(0).interestAmount());
        assertEquals(new BigDecimal("900.0"), schedule.get(0).amountRemaining());
        assertEquals(2, connection.getRequestCount());
        assertEquals("2", connection.getLastRequest().getValue("page"));
    }

    @Test
    void findRepaymentSchedule_whenMainframeRefuses_isEmpty() {
        MainframeTestConnection connection = new MainframeTestConnection();
        connection.addResponse(failedResponse());

        assertTrue(new LoanAdapter(connection).findRepaymentSchedule(LOAN).isEmpty());
    }

    @Test
    void findRepaymentSchedule_whenMainframeUnreachable_throws() {
        MainframeTestConnection connection = new MainframeTestConnection();
        connection.addResponse(networkFailure());

        assertThrows(MainframeUnavailableException.class,
                () -> new LoanAdapter(connection).findRepaymentSchedule(LOAN));
    }

    @Test
    void submitForChecking_setsStatusToPending() {
        MainframeTestConnection connection = new MainframeTestConnection();
        connection.addResponse(successResponse("loanNumber", LOAN, "status", "Pending"));

        new LoanAdapter(connection).submitForChecking(LOAN);

        assertEquals("loan.updateStatus", connection.getLastRequest().getRequestType());
        assertEquals(LOAN, connection.getLastRequest().getValue("loanNumber"));
        assertEquals("Pending", connection.getLastRequest().getValue("status"));
    }

    @Test
    void submitForChecking_whenMainframeRefuses_throwsWithItsReason() {
        MainframeTestConnection connection = new MainframeTestConnection();
        connection.addResponse(new MessageResponse(
                new Status(StatusMessage.SERVER_FAILURE.getCode(), "Loan is not New", 1), new HashMap<>()));

        RecordRejectedException e = assertThrows(RecordRejectedException.class,
                () -> new LoanAdapter(connection).submitForChecking(LOAN));
        assertEquals("Loan is not New", e.getMessage());
    }

    @Test
    void submitForChecking_whenMainframeUnreachable_throws() {
        MainframeTestConnection connection = new MainframeTestConnection();
        connection.addResponse(networkFailure());

        assertThrows(MainframeUnavailableException.class,
                () -> new LoanAdapter(connection).submitForChecking(LOAN));
    }

    private static MessageResponse schedulePage(long page, long total, long... paymentNumbers) {
        HashMap<String, String> values = new HashMap<>();
        values.put("loanNumber", LOAN);
        values.put("customerName", "Jane Smith");
        values.put("page", Long.toString(page));
        values.put("total", Long.toString(total));
        values.put("items", Integer.toString(paymentNumbers.length));
        for (int i = 0; i < paymentNumbers.length; i++) {
            String prefix = (i + 1) + ":";
            values.put(prefix + "paymentNumber", Long.toString(paymentNumbers[i]));
            values.put(prefix + "date", "2026-0" + paymentNumbers[i] + "-01");
            values.put(prefix + "amountPaid", "100.0");
            values.put(prefix + "principalAmount", "90.0");
            values.put(prefix + "interestAmount", "10.0");
            values.put(prefix + "amountRemaining", "900.0");
        }
        return successResponse(values);
    }
}
