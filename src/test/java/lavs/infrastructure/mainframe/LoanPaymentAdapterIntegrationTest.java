package lavs.infrastructure.mainframe;

import lavs.business.model.LoanPayment;
import lavs.business.repository.RecordRejectedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class LoanPaymentAdapterIntegrationTest extends MainframeIntegrationTestSupport {

    private static final String CUSTOMER_NUMBER =
            System.getProperty("mainframe.customerNumber", "01-0001-0000020");

    private static final LocalDate PAYMENT_DATE = LocalDate.of(2026, 2, 1);
    private static final BigDecimal PAYMENT_AMOUNT = new BigDecimal("100.0");

    private LoanPaymentAdapter adapter;
    private String loanNumber;

    @BeforeAll
    void setUpLoan() throws Exception {
        adapter = new LoanPaymentAdapter(connection);
        loanNumber = findOrCreateTestLoan(CUSTOMER_NUMBER);
    }

    @Test
    void addFindListAndDeletePayment() throws Exception {
        LoanPayment added = adapter.addPayment(loanNumber, PAYMENT_DATE, PAYMENT_AMOUNT);
        try {
            assertEquals(loanNumber, added.loanNumber());
            assertEquals(PAYMENT_DATE, added.date());
            assertEquals(0, PAYMENT_AMOUNT.compareTo(added.amountPaid()));

            LoanPayment found = adapter.findPayment(loanNumber, added.id()).orElseThrow();
            assertEquals(PAYMENT_DATE, found.date());
            assertEquals(0, PAYMENT_AMOUNT.compareTo(found.amountPaid()));
            assertNotNull(found.principalAmount());
            assertNotNull(found.interestAmount());
            assertNotNull(found.amountRemaining());

            List<LoanPayment> listed = fetchAllPages(
                    (page, size) -> adapter.listPayments(loanNumber, page, size));
            LoanPayment inList = listed.stream().filter(p -> p.id() == added.id()).findFirst()
                    .orElseThrow(() -> new AssertionError("Payment " + added.id() + " was not in the list"));
            assertEquals(PAYMENT_DATE, inList.date());
            assertEquals(0, PAYMENT_AMOUNT.compareTo(inList.amountPaid()));
        } finally {
            adapter.deletePayment(loanNumber, added.id());
        }

        assertTrue(adapter.findPayment(loanNumber, added.id()).isEmpty(), "Payment should be deleted");
    }

    @Test
    void findUnknownPaymentIsEmpty() {
        assertTrue(adapter.findPayment(loanNumber, Long.MAX_VALUE).isEmpty());
    }

    @Test
    void deleteUnknownPaymentIsRejected() {
        assertThrows(RecordRejectedException.class, () -> adapter.deletePayment(loanNumber, Long.MAX_VALUE));
    }

    @Test
    void listPaymentsOfUnknownLoanIsEmpty() {
        assertTrue(adapter.listPayments("99-9999-9999999-999", 1, 10).getList().isEmpty());
    }
}
