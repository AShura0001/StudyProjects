package lavs.business.repository;

import lavs.business.model.CoBorrower;
import lavs.business.model.CoBorrowerType;

import java.util.Optional;

/**
 * Reads and changes the co-borrowers and guarantors on a loan in the mainframe.
 *
 * <p>Staff add co-borrowers through drafts so it works offline; the code that
 * uploads those drafts uses {@link #addCoBorrower}.
 *
 * <p>Every method throws {@link MainframeUnavailableException} if the mainframe
 * could not be reached.
 */
public interface LoanCoBorrowerGateway {

    /**
     * Gets one co-borrower on a loan.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param id the mainframe's id for the co-borrower on the loan
     * @return the co-borrower, or empty if there is no such co-borrower
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    Optional<CoBorrower> findCoBorrower(String loanNumber, long id);

    /**
     * Gets one page of the co-borrowers on a loan.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param page the page to get, starting at 1
     * @param pageSize how many co-borrowers to put on a page
     * @return the page; empty if the loan has none or does not exist
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    PageResult<CoBorrower> listCoBorrowers(String loanNumber, long page, long pageSize);

    /**
     * Adds a customer to a loan as a co-borrower or guarantor.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param coBorrowerNumber the customer number of the co-borrower
     * @param type the part the customer plays on the loan
     * @return the co-borrower as the mainframe saved it
     * @throws RecordRejectedException if the mainframe refused the co-borrower
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    CoBorrower addCoBorrower(String loanNumber, String coBorrowerNumber, CoBorrowerType type);

    /**
     * Removes a co-borrower from a loan.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param id the mainframe's id for the co-borrower on the loan
     * @throws RecordRejectedException if the mainframe refused, for example because there is no such co-borrower
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    void deleteCoBorrower(String loanNumber, long id);
}
