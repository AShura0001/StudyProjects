package lavs.business.model;

import java.util.Objects;

/**
 * What the staff member enters about a relationship between two customers.
 *
 * <p>The description is only used when the type is {@link RelationshipType#OTHER}.
 * It is never null: a missing description is kept as an empty text, and spaces at
 * either end are removed. Whether the details make sense is checked by the
 * relationship rules, not here.
 *
 * @param type the kind of relationship
 * @param description the words describing an {@code OTHER} relationship, empty if none
 * @param relatedCustomerNumber the mainframe's number for the other customer
 */
public record RelationshipDetails(RelationshipType type, String description, String relatedCustomerNumber) {

    /**
     * Creates the details.
     *
     * @throws NullPointerException if the type or the related customer's number is null
     * @throws IllegalArgumentException if the related customer's number is blank
     */
    public RelationshipDetails {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(relatedCustomerNumber, "relatedCustomerNumber must not be null");
        if (relatedCustomerNumber.isBlank()) {
            throw new IllegalArgumentException("relatedCustomerNumber must not be blank");
        }
        description = description == null ? "" : description.strip();
    }
}
