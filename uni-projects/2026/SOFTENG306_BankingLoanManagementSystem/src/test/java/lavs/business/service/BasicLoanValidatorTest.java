package lavs.business.service;

import lavs.business.model.LoanDetails;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicLoanValidatorTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);

    private final BasicLoanValidator validator = new BasicLoanValidator();

    private static LoanDetails details(String principal, String rate, String payment, LocalDate renewal) {
        return new LoanDetails(new BigDecimal(principal), "Fixed", new BigDecimal(rate), "Yearly",
                new BigDecimal(payment), "Monthly", START, renewal);
    }

    @Test
    void validate_withSensibleValues_findsNoProblems() {
        assertTrue(validator.validate(details("250000", "5.85", "1450", START.plusYears(5))).isEmpty());
    }

    @Test
    void validate_withAPrincipalOfZeroOrLess_reportsThePrincipal() {
        assertEquals(Map.of("principal", BasicLoanValidator.PRINCIPAL_MESSAGE),
                validator.validate(details("0", "5", "100", START.plusYears(1))));
        assertEquals(Map.of("principal", BasicLoanValidator.PRINCIPAL_MESSAGE),
                validator.validate(details("-1", "5", "100", START.plusYears(1))));
    }

    @Test
    void validate_withAPrincipalJustAboveZero_isAccepted() {
        assertTrue(validator.validate(details("0.01", "5", "100", START.plusYears(1))).isEmpty());
    }

    @Test
    void validate_withARateBelowZeroOrAboveOneHundred_reportsTheRate() {
        assertEquals(Map.of("rateAmount", BasicLoanValidator.RATE_MESSAGE),
                validator.validate(details("1000", "-0.01", "100", START.plusYears(1))));
        assertEquals(Map.of("rateAmount", BasicLoanValidator.RATE_MESSAGE),
                validator.validate(details("1000", "100.01", "100", START.plusYears(1))));
    }

    @Test
    void validate_withARateAtEitherEnd_isAccepted() {
        assertTrue(validator.validate(details("1000", "0", "100", START.plusYears(1))).isEmpty());
        assertTrue(validator.validate(details("1000", "100", "100", START.plusYears(1))).isEmpty());
    }

    @Test
    void validate_withAPaymentOfZeroOrLess_reportsThePayment() {
        assertEquals(Map.of("paymentAmount", BasicLoanValidator.PAYMENT_MESSAGE),
                validator.validate(details("1000", "5", "0", START.plusYears(1))));
    }

    @Test
    void validate_withARenewalOnOrBeforeTheStart_reportsTheRenewalDate() {
        assertEquals(Map.of("renewalDate", BasicLoanValidator.RENEWAL_MESSAGE),
                validator.validate(details("1000", "5", "100", START)));
        assertEquals(Map.of("renewalDate", BasicLoanValidator.RENEWAL_MESSAGE),
                validator.validate(details("1000", "5", "100", START.minusDays(1))));
    }

    @Test
    void validate_withARenewalTheDayAfterTheStart_isAccepted() {
        assertTrue(validator.validate(details("1000", "5", "100", START.plusDays(1))).isEmpty());
    }

    @Test
    void validate_withEverythingWrong_reportsEveryProblemInOrder() {
        Map<String, String> problems = validator.validate(details("0", "500", "0", START));

        assertEquals(List.of("principal", "rateAmount", "paymentAmount", "renewalDate"),
                List.copyOf(problems.keySet()));
    }

    @Test
    void validate_withNullDetails_isRejected() {
        assertThrows(NullPointerException.class, () -> validator.validate(null));
    }
}
