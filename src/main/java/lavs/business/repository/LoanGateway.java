package lavs.business.repository;

import lavs.business.model.LoanDetails;
import lavs.business.model.LoanListing;
import lavs.business.model.LoanRecord;
import lavs.business.model.LoanSummary;
import lavs.business.model.RepaymentEntry;

import java.util.List;
import java.util.Optional;

/**
 * What {@link lavs.business.service.LoanService} needs from the mainframe for the
 * parts of a loan that only the mainframe can do, so they need a live connection.
 *
 * <p>Creating loans and co-borrowers is not here: that goes through drafts, so it
 * works offline. The business layer only knows about this interface, so tests can
 * use a mock and the data access layer decides how the mainframe is actually
 * called.
 *
 * <p>Every method throws {@link MainframeUnavailableException} if the mainframe
 * could not be reached.
 */
public interface LoanGateway {

    /**
     * Gets the summary of a loan.
     *
     * @param loanNumber the mainframe's number for the loan
     * @return the summary, or empty if there is no such loan
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    Optional<LoanSummary> findSummary(String loanNumber);

    /**
     * Gets the complete repayment schedule of a loan. If the mainframe sends the
     * schedule in pages, the implementation fetches every page before returning.
     *
     * @param loanNumber the mainframe's number for the loan
     * @return the schedule in payment order; empty if the loan has none or does not exist
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    List<RepaymentEntry> findRepaymentSchedule(String loanNumber);

    /**
     * Moves a loan from New to Pending, so it is sent for checking.
     *
     * @param loanNumber the mainframe's number for the loan
     * @throws RecordRejectedException if the mainframe refused the change, for
     *                                 example because the loan is not New
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    void submitForChecking(String loanNumber);

    /**
     * Lists every loan a customer owns.
     *
     * @param customerNumber the customer's number
     * @return the loans, oldest page first; empty if the customer has none or does not exist
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    List<LoanListing> listLoans(String customerNumber);

    /**
     * Gets a loan's owner, status and terms.
     *
     * @param loanNumber the mainframe's number for the loan
     * @return the loan, or empty if there is no such loan
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    Optional<LoanRecord> findLoan(String loanNumber);

    /**
     * Replaces a loan's terms.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param customerNumber the number of the customer who owns the loan
     * @param terms the new terms
     * @throws RecordRejectedException if the mainframe refused the change
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    void updateLoan(String loanNumber, String customerNumber, LoanDetails terms);
}
