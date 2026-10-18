package lavs.business.model;

import lavs.business.model.LoanPayloads.CoBorrowerContent;
import lavs.business.model.LoanPayloads.LoanContent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanPayloadsTest {

    private static final LoanDetails DETAILS = new LoanDetails(new BigDecimal("250000.00"), "Fixed",
            new BigDecimal("5.85"), "Yearly", new BigDecimal("1450.00"), "Monthly",
            LocalDate.of(2026, 10, 1), LocalDate.of(2031, 10, 1));
    private static final CustomerReference EXISTING = CustomerReference.existing("01-0001-0000020");
    private static final CustomerReference DRAFT = CustomerReference.draft("6f1c-draft");

    private static String loanJson() {
        return LoanPayloads.loanPayload(EXISTING, DETAILS);
    }

    @Test
    void readLoan_afterWritingForAnExistingCustomer_returnsTheSameCustomerAndDetails() {
        LoanContent content = LoanPayloads.readLoan(LoanPayloads.loanPayload(EXISTING, DETAILS));

        assertEquals(EXISTING, content.customer());
        assertEquals(DETAILS, content.details());
    }

    @Test
    void readLoan_afterWritingForADraftCustomer_returnsTheSameCustomerAndDetails() {
        LoanContent content = LoanPayloads.readLoan(LoanPayloads.loanPayload(DRAFT, DETAILS));

        assertEquals(DRAFT, content.customer());
        assertEquals(DETAILS, content.details());
    }

    @Test
    void loanPayload_writesMoneyAsTextWithItsScaleAndDatesAsIsoText() {
        String text = loanJson();

        assertTrue(text.contains("\"principal\":\"250000.00\""));
        assertTrue(text.contains("\"paymentAmount\":\"1450.00\""));
        assertTrue(text.contains("\"startDate\":\"2026-10-01\""));
        assertTrue(text.contains("\"record\":\"loan\""));
    }

    @Test
    void loanPayload_doesNotRoundAVeryPreciseAmount() {
        LoanDetails precise = new LoanDetails(new BigDecimal("0.1000000000000000055511151231257827"), "Fixed",
                new BigDecimal("5.85"), "Yearly", new BigDecimal("1450.00"), "Monthly",
                LocalDate.of(2026, 10, 1), LocalDate.of(2031, 10, 1));

        assertEquals(precise, LoanPayloads.readLoan(LoanPayloads.loanPayload(EXISTING, precise)).details());
    }

    @Test
    void loanPayload_withNullArguments_isRejected() {
        assertThrows(NullPointerException.class, () -> LoanPayloads.loanPayload(null, DETAILS));
        assertThrows(NullPointerException.class, () -> LoanPayloads.loanPayload(EXISTING, null));
    }

    @Test
    void readCoBorrower_afterWritingEachCombinationOfTypeAndCustomer_returnsThemUnchanged() {
        for (CoBorrowerType type : CoBorrowerType.values()) {
            for (CustomerReference customer : new CustomerReference[] {EXISTING, DRAFT}) {
                CoBorrowerContent content =
                        LoanPayloads.readCoBorrower(LoanPayloads.coBorrowerPayload(type, customer));

                assertEquals(type, content.type());
                assertEquals(customer, content.customer());
            }
        }
    }

    @Test
    void coBorrowerPayload_withNullArguments_isRejected() {
        assertThrows(NullPointerException.class, () -> LoanPayloads.coBorrowerPayload(null, EXISTING));
        assertThrows(NullPointerException.class,
                () -> LoanPayloads.coBorrowerPayload(CoBorrowerType.GUARANTOR, null));
    }

    @Test
    void contentRecords_withNullValues_areRejected() {
        assertThrows(NullPointerException.class, () -> new LoanContent(null, DETAILS));
        assertThrows(NullPointerException.class, () -> new LoanContent(EXISTING, null));
        assertThrows(NullPointerException.class, () -> new CoBorrowerContent(null, EXISTING));
        assertThrows(NullPointerException.class, () -> new CoBorrowerContent(CoBorrowerType.GUARANTOR, null));
    }

    @Test
    void readLoan_withNullPayload_isRejected() {
        assertThrows(NullPointerException.class, () -> LoanPayloads.readLoan(null));
    }

    @Test
    void readLoan_withTextThatIsNotJson_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> LoanPayloads.readLoan("this is not json"));
    }

    @Test
    void readLoan_withJsonThatIsNotAnObject_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> LoanPayloads.readLoan("[]"));
        assertThrows(IllegalArgumentException.class, () -> LoanPayloads.readLoan(""));
    }

    @Test
    void readLoan_withACoBorrowerPayload_isRejected() {
        String coBorrower = LoanPayloads.coBorrowerPayload(CoBorrowerType.GUARANTOR, EXISTING);

        assertThrows(IllegalArgumentException.class, () -> LoanPayloads.readLoan(coBorrower));
    }

    @Test
    void readCoBorrower_withALoanPayload_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> LoanPayloads.readCoBorrower(loanJson()));
    }

    @Test
    void readLoan_withAFieldMissing_isRejected() {
        for (String field : new String[] {"principal", "rateType", "rateAmount", "rateFrequency",
                "paymentAmount", "paymentFrequency", "startDate", "renewalDate"}) {
            String broken = loanJson().replace("\"" + field + "\"", "\"other\"");

            assertThrows(IllegalArgumentException.class, () -> LoanPayloads.readLoan(broken), field);
        }
    }

    @Test
    void readLoan_withMoneyWrittenAsANumberInsteadOfText_isRejected() {
        String broken = loanJson().replace("\"principal\":\"250000.00\"", "\"principal\":250000.00");

        assertThrows(IllegalArgumentException.class, () -> LoanPayloads.readLoan(broken));
    }

    @Test
    void readLoan_withMoneyThatIsNotANumber_isRejected() {
        String broken = loanJson().replace("250000.00", "lots");

        assertThrows(IllegalArgumentException.class, () -> LoanPayloads.readLoan(broken));
    }

    @Test
    void readLoan_withADateThatIsNotADate_isRejected() {
        String broken = loanJson().replace("2026-10-01", "1st of October");

        assertThrows(IllegalArgumentException.class, () -> LoanPayloads.readLoan(broken));
    }

    @Test
    void readLoan_withNoCustomer_isRejected() {
        String broken = loanJson().replace("\"customer\"", "\"other\"");

        assertThrows(IllegalArgumentException.class, () -> LoanPayloads.readLoan(broken));
    }

    @Test
    void readLoan_withACustomerThatIsNotAnObject_isRejected() {
        String broken = loanJson().replace("{\"kind\":\"existing\",\"number\":\"01-0001-0000020\"}", "\"C-1\"");

        assertThrows(IllegalArgumentException.class, () -> LoanPayloads.readLoan(broken));
    }

    @Test
    void readLoan_withAnUnknownCustomerKind_isRejected() {
        String broken = loanJson().replace("\"existing\"", "\"imaginary\"");

        assertThrows(IllegalArgumentException.class, () -> LoanPayloads.readLoan(broken));
    }

    @Test
    void readLoan_withABlankCustomerNumber_isRejected() {
        String broken = loanJson().replace("01-0001-0000020", " ");

        assertThrows(IllegalArgumentException.class, () -> LoanPayloads.readLoan(broken));
    }

    @Test
    void readCoBorrower_withAnUnknownType_isRejected() {
        String broken = LoanPayloads.coBorrowerPayload(CoBorrowerType.GUARANTOR, EXISTING)
                .replace("GUARANTOR", "BYSTANDER");

        assertThrows(IllegalArgumentException.class, () -> LoanPayloads.readCoBorrower(broken));
    }
}
