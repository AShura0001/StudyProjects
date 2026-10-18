package lavs.business.service;

import lavs.business.model.Relationship;
import lavs.business.model.RelationshipDetails;
import lavs.business.model.RelationshipType;
import lavs.business.repository.CustomerGateway;
import lavs.business.repository.CustomerRelationshipGateway;
import lavs.business.repository.MainframeUnavailableException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The rules for relationships between customers, such as a parent or a spouse.
 *
 * <p>A relationship links two customers by their mainframe numbers, so both must
 * already exist on the mainframe. That means every call here needs a live
 * connection. When there is none, the staff member gets a clear message. The
 * rules are checked before anything is sent, and every problem is reported at
 * once.
 *
 * <p>These calls contact the mainframe and can take a while, so a screen must call
 * them off its own thread.
 */
public class CustomerRelationshipService {

    /** The message shown when the mainframe cannot be reached. */
    public static final String OFFLINE_MESSAGE =
            "Relationships need a connection to the mainframe. Please try again when you are back online.";

    /** The key of the problem reported when a customer is related to themselves. */
    public static final String RELATED_CUSTOMER_FIELD = "relatedCustomer";

    /** The problem reported when a customer is related to themselves. */
    public static final String SELF_MESSAGE = "A customer cannot be related to themselves.";

    /** The key of the problem reported when an Other relationship has no description. */
    public static final String DESCRIPTION_FIELD = "description";

    /** The problem reported when an Other relationship has no description. */
    public static final String DESCRIPTION_MESSAGE = "An Other relationship needs a description.";

    /** The key of the problem reported when the customer cannot be found. */
    public static final String CUSTOMER_FIELD = "customer";

    /** The problem reported when the customer cannot be found. */
    public static final String CUSTOMER_NOT_FOUND_MESSAGE = "The customer could not be found.";

    /** The problem reported when the related customer cannot be found. */
    public static final String RELATED_CUSTOMER_NOT_FOUND_MESSAGE = "The related customer could not be found.";

    /** The problem reported when the relationship cannot be found. */
    public static final String RELATIONSHIP_NOT_FOUND_MESSAGE = "The relationship could not be found.";

    /** The problem reported when the mainframe did not save a relationship. */
    public static final String NOT_SAVED_MESSAGE = "The relationship could not be saved.";

    /** The problem reported when a relationship could not be deleted. */
    public static final String NOT_DELETED_MESSAGE =
            "The relationship could not be deleted. It may no longer exist.";

    @FunctionalInterface
    private interface MainframeCall<T> {
        T get() throws IOException;
    }

    private final CustomerRelationshipGateway relationships;
    private final CustomerGateway customers;

    /**
     * Creates the service.
     *
     * @param relationships how relationships are saved on the mainframe
     * @param customers how customers are looked up on the mainframe
     * @throws NullPointerException if either argument is null
     */
    public CustomerRelationshipService(CustomerRelationshipGateway relationships, CustomerGateway customers) {
        this.relationships = Objects.requireNonNull(relationships, "relationships must not be null");
        this.customers = Objects.requireNonNull(customers, "customers must not be null");
    }

    /**
     * Adds a relationship from a customer to another customer.
     *
     * @param customerNumber the mainframe's number for the customer
     * @param details the kind of relationship and the other customer
     * @return the relationship as the mainframe saved it
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if the customer number is blank
     * @throws RelationshipRuleException if a rule is broken, with every problem found
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    public Relationship add(String customerNumber, RelationshipDetails details) {
        requireCustomerNumber(customerNumber);
        Objects.requireNonNull(details, "details must not be null");
        throwIfAny(problemsInThe(customerNumber, details));
        Map<String, String> missing = new LinkedHashMap<>();
        if (!exists(customerNumber)) {
            missing.put(CUSTOMER_FIELD, CUSTOMER_NOT_FOUND_MESSAGE);
        }
        if (!exists(details.relatedCustomerNumber())) {
            missing.put(RELATED_CUSTOMER_FIELD, RELATED_CUSTOMER_NOT_FOUND_MESSAGE);
        }
        throwIfAny(missing);
        Relationship toSave = new Relationship(null, customerNumber, 0, otherTypeOf(details), null,
                details.relatedCustomerNumber(), details.type().mainframeName());
        return call(() -> relationships.addCustomerRelationship(toSave))
                .orElseThrow(() -> new RelationshipRuleException(
                        Map.of(RuleViolationException.GENERAL, NOT_SAVED_MESSAGE)));
    }

    /**
     * Changes an existing relationship.
     *
     * @param customerNumber the mainframe's number for the customer the relationship belongs to
     * @param relationshipId the mainframe's ID for the relationship
     * @param details the new kind of relationship and the other customer
     * @return the relationship as the mainframe saved it
     * @throws NullPointerException if either of the objects is null
     * @throws IllegalArgumentException if the customer number is blank
     * @throws RelationshipRuleException if a rule is broken, with every problem found
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    public Relationship update(String customerNumber, long relationshipId, RelationshipDetails details) {
        requireCustomerNumber(customerNumber);
        Objects.requireNonNull(details, "details must not be null");
        throwIfAny(problemsInThe(customerNumber, details));
        Relationship existing = find(customerNumber, relationshipId)
                .orElseThrow(() -> new RelationshipRuleException(
                        Map.of(RuleViolationException.GENERAL, RELATIONSHIP_NOT_FOUND_MESSAGE)));
        if (!exists(details.relatedCustomerNumber())) {
            throw new RelationshipRuleException(
                    Map.of(RELATED_CUSTOMER_FIELD, RELATED_CUSTOMER_NOT_FOUND_MESSAGE));
        }
        Relationship toSave = new Relationship(existing.get_customerName(), customerNumber, relationshipId,
                otherTypeOf(details), null, details.relatedCustomerNumber(), details.type().mainframeName());
        return call(() -> relationships.updateCustomerRelationship(toSave))
                .orElseThrow(() -> new RelationshipRuleException(
                        Map.of(RuleViolationException.GENERAL, NOT_SAVED_MESSAGE)));
    }

    /**
     * Deletes a relationship.
     *
     * @param customerNumber the mainframe's number for the customer the relationship belongs to
     * @param relationshipId the mainframe's ID for the relationship
     * @throws NullPointerException if the customer number is null
     * @throws IllegalArgumentException if the customer number is blank
     * @throws RelationshipRuleException if the relationship could not be deleted
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    public void delete(String customerNumber, long relationshipId) {
        requireCustomerNumber(customerNumber);
        if (!call(() -> relationships.deleteRelationshipByCustomerNumberAndId(relationshipId, customerNumber))) {
            throw new RelationshipRuleException(Map.of(RuleViolationException.GENERAL, NOT_DELETED_MESSAGE));
        }
    }

    /**
     * Looks up one relationship.
     *
     * @param customerNumber the mainframe's number for the customer the relationship belongs to
     * @param relationshipId the mainframe's ID for the relationship
     * @return the relationship, or empty if there is none
     * @throws NullPointerException if the customer number is null
     * @throws IllegalArgumentException if the customer number is blank
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    public Optional<Relationship> find(String customerNumber, long relationshipId) {
        requireCustomerNumber(customerNumber);
        return call(() -> relationships.findRelationshipByCustomerNumberAndId(relationshipId, customerNumber));
    }

    private Map<String, String> problemsInThe(String customerNumber, RelationshipDetails details) {
        Map<String, String> problems = new LinkedHashMap<>();
        if (customerNumber.equals(details.relatedCustomerNumber())) {
            problems.put(RELATED_CUSTOMER_FIELD, SELF_MESSAGE);
        }
        if (details.type() == RelationshipType.OTHER && details.description().isEmpty()) {
            problems.put(DESCRIPTION_FIELD, DESCRIPTION_MESSAGE);
        }
        return problems;
    }

    private boolean exists(String customerNumber) {
        return call(() -> customers.findByCustomerNumber(customerNumber)).isPresent();
    }

    private static String otherTypeOf(RelationshipDetails details) {
        return details.type() == RelationshipType.OTHER ? details.description() : "";
    }

    private static void throwIfAny(Map<String, String> problems) {
        if (!problems.isEmpty()) {
            throw new RelationshipRuleException(problems);
        }
    }

    private static <T> T call(MainframeCall<T> call) {
        try {
            return call.get();
        } catch (IOException e) {
            throw new MainframeUnavailableException(OFFLINE_MESSAGE, e);
        }
    }

    private static void requireCustomerNumber(String customerNumber) {
        Objects.requireNonNull(customerNumber, "customerNumber must not be null");
        if (customerNumber.isBlank()) {
            throw new IllegalArgumentException("customerNumber must not be blank");
        }
    }
}
