package lavs.business.model;

import java.util.Objects;

/**
 * A customer who is a co-borrower or guarantor on someone else's loan, as the
 * mainframe holds it.
 *
 * @param id the mainframe's id for this co-borrower on the loan
 * @param loanNumber the mainframe's number for the loan
 * @param coBorrowerNumber the customer number of the co-borrower
 * @param coBorrowerName the name of the co-borrower; may be null if the mainframe did not send it
 * @param type the part the customer plays on the loan
 */
public record CoBorrower(
        long id,
        String loanNumber,
        String coBorrowerNumber,
        String coBorrowerName,
        CoBorrowerType type) {

    /**
     * Creates the co-borrower.
     *
     * @throws NullPointerException if the loan number, co-borrower number or type is null
     */
    public CoBorrower {
        Objects.requireNonNull(loanNumber, "loanNumber must not be null");
        Objects.requireNonNull(coBorrowerNumber, "coBorrowerNumber must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }
}
