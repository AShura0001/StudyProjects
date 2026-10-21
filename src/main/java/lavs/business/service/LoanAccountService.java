package lavs.business.service;

import lavs.business.model.CoBorrower;
import lavs.business.model.CoBorrowerType;
import lavs.business.model.LoanDetails;
import lavs.business.model.LoanListing;
import lavs.business.model.LoanRecord;
import lavs.business.model.RepaymentEntry;
import lavs.business.repository.LoanCoBorrowerGateway;
import lavs.business.repository.LoanGateway;
import lavs.business.repository.LoanPaymentGateway;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.PageResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Looks after loans that are already on the mainframe: reading them, changing
 * their terms and managing their co-borrowers. Brand new loans are started with
 * {@link LoanService#createLoan}, which can work offline; everything here needs
 * the mainframe.
 */
public class LoanAccountService {

    /** Shown when the mainframe cannot be reached. */
    public static final String OFFLINE_MESSAGE = "The mainframe could not be reached. Try again.";

    private static final long PAGE_SIZE = 100;
    private static final int MAX_PAGES = 1000;

    private final LoanGateway loans;
    private final LoanCoBorrowerGateway coBorrowers;
    private final LoanPaymentGateway payments;
    private final LoanValidator validator;

    /**
     * @throws NullPointerException if any argument is null
     */
    public LoanAccountService(LoanGateway loans, LoanCoBorrowerGateway coBorrowers,
                              LoanPaymentGateway payments, LoanValidator validator) {
        this.loans = Objects.requireNonNull(loans, "loans must not be null");
        this.coBorrowers = Objects.requireNonNull(coBorrowers, "coBorrowers must not be null");
        this.payments = Objects.requireNonNull(payments, "payments must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
    }

    /** @return every loan the customer owns; empty when they have none */
    public List<LoanListing> loansOf(String customerNumber) {
        requireText(customerNumber, "customerNumber");
        return offline(() -> loans.listLoans(customerNumber));
    }

    /** @return the loan, or empty when the mainframe does not know the number */
    public Optional<LoanRecord> find(String loanNumber) {
        requireText(loanNumber, "loanNumber");
        return offline(() -> loans.findLoan(loanNumber));
    }

    /** @return the repayment schedule in payment order; empty when there is none */
    public List<RepaymentEntry> scheduleOf(String loanNumber) {
        requireText(loanNumber, "loanNumber");
        return offline(() -> loans.findRepaymentSchedule(loanNumber));
    }

    /** @return how many repayments have been recorded against the loan */
    public long paymentsMade(String loanNumber) {
        requireText(loanNumber, "loanNumber");
        return offline(() -> payments.listPayments(loanNumber, 1, 1).getTotal());
    }

    /** @return the loan's co-borrowers and guarantors */
    public List<CoBorrower> coBorrowersOf(String loanNumber) {
        requireText(loanNumber, "loanNumber");
        return offline(() -> {
            List<CoBorrower> all = new ArrayList<>();
            for (long page = 1; page <= MAX_PAGES; page++) {
                PageResult<CoBorrower> result = coBorrowers.listCoBorrowers(loanNumber, page, PAGE_SIZE);
                all.addAll(result.getList());
                if (result.getList().isEmpty() || all.size() >= result.getTotal()) {
                    break;
                }
            }
            return all;
        });
    }

    /**
     * Replaces a loan's terms after checking them.
     *
     * @return the loan as the mainframe now holds it
     * @throws LoanRuleException if the loan does not exist or the terms break a rule
     * @throws lavs.business.repository.RecordRejectedException if the mainframe refused the change
     */
    public LoanRecord updateTerms(String loanNumber, LoanDetails terms) {
        requireText(loanNumber, "loanNumber");
        Objects.requireNonNull(terms, "terms must not be null");
        LoanRecord loan = find(loanNumber).orElseThrow(() ->
                new LoanRuleException(Map.of(LoanRuleException.GENERAL, LoanService.LOAN_NOT_FOUND_MESSAGE)));
        Map<String, String> problems = validator.validate(terms);
        if (!problems.isEmpty()) {
            throw new LoanRuleException(problems);
        }
        offline(() -> {
            loans.updateLoan(loanNumber, loan.customerNumber(), terms);
            return null;
        });
        return find(loanNumber).orElse(loan);
    }

    /**
     * Adds a co-borrower or guarantor to a loan.
     *
     * @throws LoanRuleException if the loan does not exist or the customer owns it
     * @throws lavs.business.repository.RecordRejectedException if the mainframe refused, for
     *                                                          example because the customer does not exist
     */
    public CoBorrower addCoBorrower(String loanNumber, String customerNumber, CoBorrowerType type) {
        requireText(loanNumber, "loanNumber");
        requireText(customerNumber, "customerNumber");
        Objects.requireNonNull(type, "type must not be null");
        LoanRecord loan = find(loanNumber).orElseThrow(() ->
                new LoanRuleException(Map.of(LoanRuleException.GENERAL, LoanService.LOAN_NOT_FOUND_MESSAGE)));
        if (customerNumber.equalsIgnoreCase(loan.customerNumber())) {
            throw new LoanRuleException(Map.of(LoanService.CO_BORROWER_FIELD, LoanService.OWN_LOAN_MESSAGE));
        }
        return offline(() -> coBorrowers.addCoBorrower(loanNumber, customerNumber, type));
    }

    /**
     * The loan's terms as one value, ready to change and pass to {@link #updateTerms}.
     *
     * @return the terms, or empty when the mainframe left one of them out
     */
    public static Optional<LoanDetails> termsOf(LoanRecord loan) {
        if (loan.principal() == null || loan.rateType() == null || loan.rateAmount() == null
                || loan.rateFrequency() == null || loan.paymentAmount() == null || loan.paymentFrequency() == null
                || loan.startDate() == null || loan.renewalDate() == null) {
            return Optional.empty();
        }
        return Optional.of(new LoanDetails(loan.principal(), loan.rateType(), loan.rateAmount(), loan.rateFrequency(),
                loan.paymentAmount(), loan.paymentFrequency(), loan.startDate(), loan.renewalDate()));
    }

    /** Takes a co-borrower or guarantor off a loan. */
    public void removeCoBorrower(String loanNumber, long coBorrowerId) {
        requireText(loanNumber, "loanNumber");
        offline(() -> {
            coBorrowers.deleteCoBorrower(loanNumber, coBorrowerId);
            return null;
        });
    }

    private interface Call<T> {
        T run();
    }

    /** Gives every "cannot reach the mainframe" the same message staff can act on. */
    private static <T> T offline(Call<T> call) {
        try {
            return call.run();
        } catch (MainframeUnavailableException e) {
            throw new MainframeUnavailableException(OFFLINE_MESSAGE, e);
        }
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
