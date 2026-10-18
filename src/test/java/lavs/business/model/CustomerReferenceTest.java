package lavs.business.model;

import lavs.business.model.CustomerReference.DraftCustomer;
import lavs.business.model.CustomerReference.ExistingCustomer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerReferenceTest {

    @Test
    void existing_withANumber_refersToThatMainframeCustomer() {
        CustomerReference reference = CustomerReference.existing("01-0001-0000020");

        assertEquals("01-0001-0000020", assertInstanceOf(ExistingCustomer.class, reference).customerNumber());
    }

    @Test
    void draft_withAnId_refersToThatDraftCustomer() {
        CustomerReference reference = CustomerReference.draft("draft-1");

        assertEquals("draft-1", assertInstanceOf(DraftCustomer.class, reference).draftId());
    }

    @Test
    void existing_withNullOrBlankNumber_isRejected() {
        assertThrows(NullPointerException.class, () -> CustomerReference.existing(null));
        assertThrows(IllegalArgumentException.class, () -> CustomerReference.existing(" "));
    }

    @Test
    void draft_withNullOrBlankId_isRejected() {
        assertThrows(NullPointerException.class, () -> CustomerReference.draft(null));
        assertThrows(IllegalArgumentException.class, () -> CustomerReference.draft(""));
    }

    @Test
    void equals_forTheSameCustomer_isTrue() {
        assertEquals(CustomerReference.existing("C-1"), CustomerReference.existing("C-1"));
        assertEquals(CustomerReference.draft("d-1"), CustomerReference.draft("d-1"));
    }

    @Test
    void equals_forDifferentCustomers_isFalse() {
        assertNotEquals(CustomerReference.existing("C-1"), CustomerReference.existing("C-2"));
        assertNotEquals(CustomerReference.draft("d-1"), CustomerReference.draft("d-2"));
    }

    @Test
    void equals_forAnExistingAndADraftWithTheSameText_isFalse() {
        assertNotEquals(CustomerReference.existing("same"), CustomerReference.draft("same"));
    }
}
