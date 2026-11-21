package lavs.business.repository;

import lavs.business.model.LoanPayment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Reads and changes the payments made on a loan in the mainframe.
 *
 * <p>Every method throws {@link MainframeUnavailableException} if the mainframe
 * could not be reached.
 */
public interface LoanPaymentGateway {

    /**
     * Gets one payment on a loan, including how it was split.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param id the mainframe's id for the payment on the loan
     * @return the payment, or empty if there is no such payment
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    Optional<LoanPayment> findPayment(String loanNumber, long id);

    /**
     * Gets one page of the payments on a loan. The mainframe only sends the date
     * and amount in a list, so the split and amount remaining are null.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param page the page to get, starting at 1
     * @param pageSize how many payments to put on a page
     * @return the page; empty if the loan has none or does not exist
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    PageResult<LoanPayment> listPayments(String loanNumber, long page, long pageSize);

    /**
     * Records a payment on a loan.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param date the day the payment was made
     * @param amountPaid the amount of the payment
     * @return the payment as the mainframe saved it, including how it was split
     * @throws RecordRejectedException if the mainframe refused the payment
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    LoanPayment addPayment(String loanNumber, LocalDate date, BigDecimal amountPaid);

    /**
     * Removes a payment from a loan.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param id the mainframe's id for the payment on the loan
     * @throws RecordRejectedException if the mainframe refused, for example because there is no such payment
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    void deletePayment(String loanNumber, long id);
}
