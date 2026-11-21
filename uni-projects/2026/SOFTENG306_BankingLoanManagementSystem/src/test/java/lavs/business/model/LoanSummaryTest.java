package lavs.business.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoanSummaryTest {

    private static final BigDecimal TOTAL = new BigDecimal("348000.00");
    private static final BigDecimal INTEREST = new BigDecimal("98000.00");
    private static final LocalDate FINAL_DATE = LocalDate.of(2056, 10, 1);
    private static final BigDecimal FINAL_AMOUNT = new BigDecimal("1449.80");

    @Test
    void newLoanSummary_withAllValues_keepsThem() {
        LoanSummary summary = new LoanSummary("LN-1", "Jane Doe", 360, TOTAL, INTEREST, FINAL_DATE, FINAL_AMOUNT);

        assertEquals("LN-1", summary.loanNumber());
        assertEquals("Jane Doe", summary.customerName());
        assertEquals(360, summary.numberOfPayments());
        assertEquals(TOTAL, summary.totalAmountPaid());
        assertEquals(INTEREST, summary.totalInterestPaid());
        assertEquals(FINAL_DATE, summary.finalPaymentDate());
        assertEquals(FINAL_AMOUNT, summary.finalPaymentAmount());
    }

    @Test
    void newLoanSummary_withAnyObjectValueNull_isRejected() {
        assertThrows(NullPointerException.class,
                () -> new LoanSummary(null, "Jane", 1, TOTAL, INTEREST, FINAL_DATE, FINAL_AMOUNT));
        assertThrows(NullPointerException.class,
                () -> new LoanSummary("LN-1", null, 1, TOTAL, INTEREST, FINAL_DATE, FINAL_AMOUNT));
        assertThrows(NullPointerException.class,
                () -> new LoanSummary("LN-1", "Jane", 1, null, INTEREST, FINAL_DATE, FINAL_AMOUNT));
        assertThrows(NullPointerException.class,
                () -> new LoanSummary("LN-1", "Jane", 1, TOTAL, null, FINAL_DATE, FINAL_AMOUNT));
        assertThrows(NullPointerException.class,
                () -> new LoanSummary("LN-1", "Jane", 1, TOTAL, INTEREST, null, FINAL_AMOUNT));
        assertThrows(NullPointerException.class,
                () -> new LoanSummary("LN-1", "Jane", 1, TOTAL, INTEREST, FINAL_DATE, null));
    }
}
