package lavs.business.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepaymentEntryTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 1);
    private static final BigDecimal PAID = new BigDecimal("1450.00");
    private static final BigDecimal PRINCIPAL = new BigDecimal("400.00");
    private static final BigDecimal INTEREST = new BigDecimal("1050.00");
    private static final BigDecimal REMAINING = new BigDecimal("249600.00");

    @Test
    void newRepaymentEntry_withAllValues_keepsThem() {
        RepaymentEntry entry = new RepaymentEntry(1, DATE, PAID, PRINCIPAL, INTEREST, REMAINING);

        assertEquals(1, entry.paymentNumber());
        assertEquals(DATE, entry.date());
        assertEquals(PAID, entry.amountPaid());
        assertEquals(PRINCIPAL, entry.principalAmount());
        assertEquals(INTEREST, entry.interestAmount());
        assertEquals(REMAINING, entry.amountRemaining());
    }

    @Test
    void newRepaymentEntry_withAnyObjectValueNull_isRejected() {
        assertThrows(NullPointerException.class,
                () -> new RepaymentEntry(1, null, PAID, PRINCIPAL, INTEREST, REMAINING));
        assertThrows(NullPointerException.class,
                () -> new RepaymentEntry(1, DATE, null, PRINCIPAL, INTEREST, REMAINING));
        assertThrows(NullPointerException.class,
                () -> new RepaymentEntry(1, DATE, PAID, null, INTEREST, REMAINING));
        assertThrows(NullPointerException.class,
                () -> new RepaymentEntry(1, DATE, PAID, PRINCIPAL, null, REMAINING));
        assertThrows(NullPointerException.class,
                () -> new RepaymentEntry(1, DATE, PAID, PRINCIPAL, INTEREST, null));
    }
}
