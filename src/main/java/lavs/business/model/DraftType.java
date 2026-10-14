package lavs.business.model;

/**
 * The kind of record a {@link Draft} holds.
 */
public enum DraftType {

    /** A customer and their details. */
    CUSTOMER,

    /** A loan for a customer. */
    LOAN,

    /** A co-borrower attached to a loan. */
    COBORROWER
}
