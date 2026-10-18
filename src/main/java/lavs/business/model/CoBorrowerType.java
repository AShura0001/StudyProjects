package lavs.business.model;

/**
 * The part a customer plays on a loan they do not own. These are the two types
 * the mainframe accepts.
 */
public enum CoBorrowerType {

    /** Borrows together with the loan's owner. */
    COBORROWER,

    /** Guarantees the loan, for example because the owner has no job. */
    GUARANTOR
}
