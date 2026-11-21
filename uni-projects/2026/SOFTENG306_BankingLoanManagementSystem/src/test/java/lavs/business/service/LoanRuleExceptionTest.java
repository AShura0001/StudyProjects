package lavs.business.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoanRuleExceptionTest {

    @Test
    void newException_withOneProblem_usesItAsTheMessage() {
        LoanRuleException exception = new LoanRuleException(Map.of("principal", "Too small."));

        assertEquals("Too small.", exception.getMessage());
        assertEquals(Map.of("principal", "Too small."), exception.getProblems());
    }

    @Test
    void newException_withSeveralProblems_joinsTheMessagesAndKeepsTheirOrder() {
        Map<String, String> problems = new LinkedHashMap<>();
        problems.put("mailingAddress", "No address.");
        problems.put("principal", "Too small.");
        problems.put("rate", "Too high.");

        LoanRuleException exception = new LoanRuleException(problems);

        assertEquals("No address. Too small. Too high.", exception.getMessage());
        assertEquals(List.of("mailingAddress", "principal", "rate"), List.copyOf(exception.getProblems().keySet()));
    }

    @Test
    void getProblems_whenTheOriginalMapChangesLater_isNotAffected() {
        Map<String, String> problems = new LinkedHashMap<>();
        problems.put("principal", "Too small.");
        LoanRuleException exception = new LoanRuleException(problems);

        problems.put("rate", "Too high.");

        assertEquals(Map.of("principal", "Too small."), exception.getProblems());
    }

    @Test
    void getProblems_whenCallerTriesToChangeThem_isRejected() {
        LoanRuleException exception = new LoanRuleException(Map.of("principal", "Too small."));

        assertThrows(UnsupportedOperationException.class, () -> exception.getProblems().put("rate", "Too high."));
    }

    @Test
    void newException_withNoProblems_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LoanRuleException(Map.of()));
    }

    @Test
    void newException_withNullProblems_isRejected() {
        assertThrows(NullPointerException.class, () -> new LoanRuleException(null));
    }

    @Test
    void general_isTheKeyForAProblemThatIsNotAboutOneField() {
        assertEquals("general", LoanRuleException.GENERAL);
    }
}
