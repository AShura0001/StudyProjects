package lavs.business.service;

import lavs.business.model.LoanSummary;
import lavs.business.model.RepaymentEntry;
import lavs.business.repository.LoanGateway;
import lavs.business.repository.MainframeUnavailableException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The rules for loans.
 *
 * <p>Creating a loan or a co-borrower is saved as a draft, so it works without a
 * connection. The loan summary and the repayment schedule are worked out by the
 * mainframe, so asking for them needs a live connection. When there is none, the
 * staff member gets a clear message straight away.
 *
 * <p>These calls contact the mainframe and can take a while, so a screen must
 * call them off its own thread.
 */
public class LoanService {

    /** The message shown when the summary is asked for without a connection. */
    public static final String SUMMARY_OFFLINE_MESSAGE =
            "The loan summary needs a connection to the mainframe. Please try again when you are back online.";

    /** The message shown when the schedule is asked for without a connection. */
    public static final String SCHEDULE_OFFLINE_MESSAGE =
            "The repayment schedule needs a connection to the mainframe. Please try again when you are back online.";

    private final LoanGateway gateway;

    /**
     * Creates a service that gets the mainframe's figures through the given gateway.
     *
     * @param gateway how the mainframe is asked about loans
     * @throws NullPointerException if the gateway is null
     */
    public LoanService(LoanGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway must not be null");
    }

    /**
     * Gets the summary of a loan. This needs a connection to the mainframe.
     *
     * @param loanNumber the mainframe's number for the loan
     * @return the summary, or empty if there is no such loan
     * @throws NullPointerException if the loan number is null
     * @throws IllegalArgumentException if the loan number is blank
     * @throws MainframeUnavailableException with a message that can be shown to the
     *                                       staff member, if the mainframe could not be reached
     */
    public Optional<LoanSummary> summaryOf(String loanNumber) {
        requireLoanNumber(loanNumber);
        try {
            return gateway.findSummary(loanNumber);
        } catch (MainframeUnavailableException e) {
            throw new MainframeUnavailableException(SUMMARY_OFFLINE_MESSAGE, e);
        }
    }

    /**
     * Gets the repayment schedule of a loan. This needs a connection to the mainframe.
     *
     * @param loanNumber the mainframe's number for the loan
     * @return the schedule in payment order; empty if the loan has none or does not exist
     * @throws NullPointerException if the loan number is null
     * @throws IllegalArgumentException if the loan number is blank
     * @throws MainframeUnavailableException with a message that can be shown to the
     *                                       staff member, if the mainframe could not be reached
     */
    public List<RepaymentEntry> repaymentScheduleOf(String loanNumber) {
        requireLoanNumber(loanNumber);
        try {
            return gateway.findRepaymentSchedule(loanNumber);
        } catch (MainframeUnavailableException e) {
            throw new MainframeUnavailableException(SCHEDULE_OFFLINE_MESSAGE, e);
        }
    }

    private static void requireLoanNumber(String loanNumber) {
        Objects.requireNonNull(loanNumber, "loanNumber must not be null");
        if (loanNumber.isBlank()) {
            throw new IllegalArgumentException("loanNumber must not be blank");
        }
    }
}
