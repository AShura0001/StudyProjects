package lavs.business.service;

import lavs.business.model.LoanSummary;
import lavs.business.model.RepaymentEntry;
import lavs.business.repository.LoanGateway;
import lavs.business.repository.MainframeUnavailableException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoanServiceTest {

    private static final LoanSummary SUMMARY = new LoanSummary("LN-1", "Jane Doe", 360,
            new BigDecimal("348000.00"), new BigDecimal("98000.00"), LocalDate.of(2056, 10, 1),
            new BigDecimal("1449.80"));

    private static RepaymentEntry entry(long number) {
        return new RepaymentEntry(number, LocalDate.of(2026, 10, 1).plusMonths(number),
                new BigDecimal("1450.00"), new BigDecimal("400.00"), new BigDecimal("1050.00"),
                new BigDecimal("249600.00"));
    }

    private final LoanGateway gateway = mock(LoanGateway.class);
    private final LoanService service = new LoanService(gateway);

    @Test
    void newService_withNullGateway_isRejected() {
        assertThrows(NullPointerException.class, () -> new LoanService(null));
    }

    @Test
    void summaryOf_withAKnownLoan_returnsTheMainframesSummary() {
        when(gateway.findSummary("LN-1")).thenReturn(Optional.of(SUMMARY));

        assertEquals(Optional.of(SUMMARY), service.summaryOf("LN-1"));
    }

    @Test
    void summaryOf_withAnUnknownLoan_returnsEmpty() {
        when(gateway.findSummary("LN-9")).thenReturn(Optional.empty());

        assertTrue(service.summaryOf("LN-9").isEmpty());
    }

    @Test
    void summaryOf_whenTheMainframeCannotBeReached_givesAClearMessageAndKeepsTheCause() {
        MainframeUnavailableException cause = new MainframeUnavailableException("connection refused");
        when(gateway.findSummary("LN-1")).thenThrow(cause);

        MainframeUnavailableException thrown =
                assertThrows(MainframeUnavailableException.class, () -> service.summaryOf("LN-1"));

        assertEquals(LoanService.SUMMARY_OFFLINE_MESSAGE, thrown.getMessage());
        assertSame(cause, thrown.getCause());
    }

    @Test
    void summaryOf_withANullLoanNumber_isRejectedWithoutAskingTheMainframe() {
        assertThrows(NullPointerException.class, () -> service.summaryOf(null));

        verifyNoInteractions(gateway);
    }

    @Test
    void summaryOf_withABlankLoanNumber_isRejectedWithoutAskingTheMainframe() {
        assertThrows(IllegalArgumentException.class, () -> service.summaryOf("  "));

        verifyNoInteractions(gateway);
    }

    @Test
    void summaryOf_asksTheMainframeOnceWithTheNumberAsGiven() {
        when(gateway.findSummary("LN-1")).thenReturn(Optional.of(SUMMARY));

        service.summaryOf("LN-1");

        verify(gateway).findSummary("LN-1");
    }

    @Test
    void repaymentScheduleOf_withAKnownLoan_returnsTheEntriesInOrder() {
        List<RepaymentEntry> schedule = List.of(entry(1), entry(2), entry(3));
        when(gateway.findRepaymentSchedule("LN-1")).thenReturn(schedule);

        assertEquals(schedule, service.repaymentScheduleOf("LN-1"));
    }

    @Test
    void repaymentScheduleOf_withNoEntries_returnsAnEmptyList() {
        when(gateway.findRepaymentSchedule("LN-1")).thenReturn(List.of());

        assertTrue(service.repaymentScheduleOf("LN-1").isEmpty());
    }

    @Test
    void repaymentScheduleOf_whenTheMainframeCannotBeReached_givesAClearMessageAndKeepsTheCause() {
        MainframeUnavailableException cause = new MainframeUnavailableException("timed out", new IOException());
        when(gateway.findRepaymentSchedule("LN-1")).thenThrow(cause);

        MainframeUnavailableException thrown =
                assertThrows(MainframeUnavailableException.class, () -> service.repaymentScheduleOf("LN-1"));

        assertEquals(LoanService.SCHEDULE_OFFLINE_MESSAGE, thrown.getMessage());
        assertSame(cause, thrown.getCause());
    }

    @Test
    void repaymentScheduleOf_withANullLoanNumber_isRejectedWithoutAskingTheMainframe() {
        assertThrows(NullPointerException.class, () -> service.repaymentScheduleOf(null));

        verifyNoInteractions(gateway);
    }

    @Test
    void repaymentScheduleOf_withABlankLoanNumber_isRejectedWithoutAskingTheMainframe() {
        assertThrows(IllegalArgumentException.class, () -> service.repaymentScheduleOf(""));

        verifyNoInteractions(gateway);
    }

    @Test
    void offlineMessages_saySoInPlainWordsAndNameWhatNeedsTheConnection() {
        assertTrue(LoanService.SUMMARY_OFFLINE_MESSAGE.contains("summary"));
        assertTrue(LoanService.SCHEDULE_OFFLINE_MESSAGE.contains("schedule"));
    }
}
