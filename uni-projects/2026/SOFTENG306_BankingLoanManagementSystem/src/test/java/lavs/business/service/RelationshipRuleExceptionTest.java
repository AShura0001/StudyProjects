package lavs.business.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelationshipRuleExceptionTest {

    @Test
    void newException_withProblems_keepsThemAndUsesThemAsTheMessage() {
        RelationshipRuleException exception = new RelationshipRuleException(Map.of("relatedCustomer", "Nope."));

        assertEquals("Nope.", exception.getMessage());
        assertEquals(Map.of("relatedCustomer", "Nope."), exception.getProblems());
    }

    @Test
    void newException_withNoProblems_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RelationshipRuleException(Map.of()));
    }

    @Test
    void relationshipAndLoanRuleExceptions_shareTheSameBaseSoOneHandlerCanShowEither() {
        assertInstanceOf(RuleViolationException.class, new RelationshipRuleException(Map.of("a", "b")));
        assertInstanceOf(RuleViolationException.class, new LoanRuleException(Map.of("a", "b")));
    }

    @Test
    void general_isTheSameKeyForBoth() {
        assertEquals(RuleViolationException.GENERAL, LoanRuleException.GENERAL);
    }
}
