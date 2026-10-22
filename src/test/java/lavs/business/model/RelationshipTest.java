package lavs.business.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RelationshipTest {

    @Test
    void newRelationship_withAllValues_keepsThem() {
        Relationship relationship = new Relationship("Jane Doe", "C-1", 7L, "friend", "Sam Doe", "C-2", "Other");

        assertEquals("Jane Doe", relationship.get_customerName());
        assertEquals("C-1", relationship.get_customerNumber());
        assertEquals(7L, relationship.get_id());
        assertEquals("friend", relationship.get_otherType());
        assertEquals("Sam Doe", relationship.get_secondaryCustomerName());
        assertEquals("C-2", relationship.get_secondaryCustomerNumber());
        assertEquals("Other", relationship.get_type());
    }

    @Test
    void setters_changeEveryProperty() {
        Relationship relationship = new Relationship();

        relationship.set_customerName("Alex Doe");
        relationship.set_customerNumber("C-3");
        relationship.set_id(21L);
        relationship.set_number("R-1");
        relationship.set_otherType("neighbour");
        relationship.set_secondaryCustomerName("Pat Doe");
        relationship.set_secondaryCustomerNumber("C-4");
        relationship.set_type("Spouse");

        assertEquals("Alex Doe", relationship.get_customerName());
        assertEquals("C-3", relationship.get_customerNumber());
        assertEquals(21L, relationship.get_id());
        assertEquals("R-1", relationship.get_number());
        assertEquals("neighbour", relationship.get_otherType());
        assertEquals("Pat Doe", relationship.get_secondaryCustomerName());
        assertEquals("C-4", relationship.get_secondaryCustomerNumber());
        assertEquals("Spouse", relationship.get_type());
    }

    @Test
    void relationship_isACustomerType() {
        assertInstanceOf(CustomerType.class, new Relationship());
    }
}
