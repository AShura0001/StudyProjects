package lavs.business.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the records that hold a loan's co-borrowers, documents, notes and payments. */
class LoanRecordsTest {

    private static final String LOAN = "01-0001-0000020-001";
    private static final LocalDate DATE = LocalDate.of(2026, 10, 1);
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");

    @Test
    void newCoBorrower_keepsValues() {
        CoBorrower coBorrower = new CoBorrower(3, LOAN, "01-0001-0000015", "Jane Smith", CoBorrowerType.GUARANTOR);

        assertEquals(3, coBorrower.id());
        assertEquals(LOAN, coBorrower.loanNumber());
        assertEquals("01-0001-0000015", coBorrower.coBorrowerNumber());
        assertEquals("Jane Smith", coBorrower.coBorrowerName());
        assertEquals(CoBorrowerType.GUARANTOR, coBorrower.type());
    }

    @Test
    void newCoBorrower_withRequiredValueNull_isRejected() {
        assertThrows(NullPointerException.class,
                () -> new CoBorrower(1, null, "C", null, CoBorrowerType.COBORROWER));
        assertThrows(NullPointerException.class,
                () -> new CoBorrower(1, LOAN, null, null, CoBorrowerType.COBORROWER));
        assertThrows(NullPointerException.class,
                () -> new CoBorrower(1, LOAN, "C", null, null));
    }

    @Test
    void newLoanDocument_keepsValuesAndAllowsMissingFileDetails() {
        LoanDocument full = new LoanDocument(4, LOAN, "income.pdf", "application/pdf", 2048L, "Income");
        LoanDocument fromList = new LoanDocument(4, LOAN, "income.pdf", null, null, "Income");

        assertEquals(4, full.id());
        assertEquals(LOAN, full.loanNumber());
        assertEquals("income.pdf", full.fileName());
        assertEquals("application/pdf", full.fileType());
        assertEquals(2048L, full.size());
        assertEquals("Income", full.type());
        assertNull(fromList.fileType());
        assertNull(fromList.size());
    }

    @Test
    void newLoanDocument_withRequiredValueNull_isRejected() {
        assertThrows(NullPointerException.class, () -> new LoanDocument(1, null, "f", null, null, "t"));
        assertThrows(NullPointerException.class, () -> new LoanDocument(1, LOAN, null, null, null, "t"));
        assertThrows(NullPointerException.class, () -> new LoanDocument(1, LOAN, "f", null, null, null));
    }

    @Test
    void newLoanNote_keepsValuesAndAllowsMissingText() {
        LoanNote note = new LoanNote(5, LOAN, "Review", "test", "2026-10-01", "Line 1");
        LoanNote fromList = new LoanNote(5, LOAN, "Review", null, null, null);

        assertEquals(5, note.id());
        assertEquals(LOAN, note.loanNumber());
        assertEquals("Review", note.type());
        assertEquals("test", note.author());
        assertEquals("2026-10-01", note.dateAdded());
        assertEquals("Line 1", note.lines());
        assertNull(fromList.lines());
    }

    @Test
    void newLoanNote_withRequiredValueNull_isRejected() {
        assertThrows(NullPointerException.class, () -> new LoanNote(1, null, "t", null, null, null));
        assertThrows(NullPointerException.class, () -> new LoanNote(1, LOAN, null, null, null, null));
    }

    @Test
    void newLoanPayment_keepsValuesAndAllowsMissingSplit() {
        LoanPayment payment = new LoanPayment(6, LOAN, DATE, AMOUNT,
                new BigDecimal("90.00"), new BigDecimal("10.00"), new BigDecimal("900.00"));
        LoanPayment fromList = new LoanPayment(6, LOAN, DATE, AMOUNT, null, null, null);

        assertEquals(6, payment.id());
        assertEquals(LOAN, payment.loanNumber());
        assertEquals(DATE, payment.date());
        assertEquals(AMOUNT, payment.amountPaid());
        assertEquals(new BigDecimal("90.00"), payment.principalAmount());
        assertEquals(new BigDecimal("10.00"), payment.interestAmount());
        assertEquals(new BigDecimal("900.00"), payment.amountRemaining());
        assertNull(fromList.principalAmount());
    }

    @Test
    void newLoanPayment_withRequiredValueNull_isRejected() {
        assertThrows(NullPointerException.class, () -> new LoanPayment(1, null, DATE, AMOUNT, null, null, null));
        assertThrows(NullPointerException.class, () -> new LoanPayment(1, LOAN, null, AMOUNT, null, null, null));
        assertThrows(NullPointerException.class, () -> new LoanPayment(1, LOAN, DATE, null, null, null, null));
    }
}
