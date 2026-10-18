package lavs.business.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoanDetailsTest {

    private static final BigDecimal MONEY = new BigDecimal("250000.00");
    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final LocalDate RENEWAL = LocalDate.of(2031, 10, 1);

    private static LoanDetails valid() {
        return new LoanDetails(MONEY, "Fixed", new BigDecimal("5.85"), "Yearly",
                new BigDecimal("1450.00"), "Monthly", START, RENEWAL);
    }

    @Test
    void newLoanDetails_withAllValues_keepsThem() {
        LoanDetails details = valid();

        assertEquals(MONEY, details.principal());
        assertEquals("Fixed", details.rateType());
        assertEquals(new BigDecimal("5.85"), details.rateAmount());
        assertEquals("Yearly", details.rateFrequency());
        assertEquals(new BigDecimal("1450.00"), details.paymentAmount());
        assertEquals("Monthly", details.paymentFrequency());
        assertEquals(START, details.startDate());
        assertEquals(RENEWAL, details.renewalDate());
    }

    @Test
    void newLoanDetails_withTheSameValues_areEqual() {
        assertEquals(valid(), valid());
    }

    @Test
    void newLoanDetails_withAnyValueNull_isRejected() {
        BigDecimal rate = new BigDecimal("5.85");
        BigDecimal payment = new BigDecimal("1450.00");
        assertThrows(NullPointerException.class,
                () -> new LoanDetails(null, "Fixed", rate, "Yearly", payment, "Monthly", START, RENEWAL));
        assertThrows(NullPointerException.class,
                () -> new LoanDetails(MONEY, null, rate, "Yearly", payment, "Monthly", START, RENEWAL));
        assertThrows(NullPointerException.class,
                () -> new LoanDetails(MONEY, "Fixed", null, "Yearly", payment, "Monthly", START, RENEWAL));
        assertThrows(NullPointerException.class,
                () -> new LoanDetails(MONEY, "Fixed", rate, null, payment, "Monthly", START, RENEWAL));
        assertThrows(NullPointerException.class,
                () -> new LoanDetails(MONEY, "Fixed", rate, "Yearly", null, "Monthly", START, RENEWAL));
        assertThrows(NullPointerException.class,
                () -> new LoanDetails(MONEY, "Fixed", rate, "Yearly", payment, null, START, RENEWAL));
        assertThrows(NullPointerException.class,
                () -> new LoanDetails(MONEY, "Fixed", rate, "Yearly", payment, "Monthly", null, RENEWAL));
        assertThrows(NullPointerException.class,
                () -> new LoanDetails(MONEY, "Fixed", rate, "Yearly", payment, "Monthly", START, null));
    }

    @Test
    void newLoanDetails_withNonsenseButPresentValues_isNotTheJobOfThisClassToRefuse() {
        LoanDetails details = new LoanDetails(new BigDecimal("-1"), "x", BigDecimal.ZERO, "y",
                BigDecimal.ZERO, "z", START, START.minusYears(1));

        assertEquals(new BigDecimal("-1"), details.principal());
    }
}
