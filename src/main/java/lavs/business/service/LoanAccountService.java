package lavs.business.service;

import lavs.business.model.CoBorrower;
import lavs.business.model.CoBorrowerType;
import lavs.business.model.CustomerProfile;
import lavs.business.model.CustomerReference;
import lavs.business.model.LoanDetails;
import lavs.business.model.LoanListing;
import lavs.business.model.LoanRecord;
import lavs.business.model.RepaymentEntry;
import lavs.business.model.RepaymentProgress;
import lavs.business.model.RepaymentStatus;
import lavs.business.repository.LoanCoBorrowerGateway;
import lavs.business.repository.LoanGateway;
import lavs.business.repository.LoanPaymentGateway;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.PageResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    /** The status a loan has until it is submitted for checking. */
    public static final String NEW_STATUS = "New";
    /** The key of the problem reported when the loan is not New. */
    public static final String STATUS_FIELD = "status";
    /** The problem reported when the loan is not New. */
    public static final String STATUS_MESSAGE = "Only a loan with the status New can be submitted for checking.";
    /** The key of the problem reported when the mainframe left some details of the loan out. */
    public static final String DETAILS_FIELD = "details";
    /** The problem reported when the mainframe left some details of the loan out. */
    public static final String DETAILS_MESSAGE =
            "The loan is missing some of its details, so it cannot be submitted for checking.";
    /** The key of the problem reported when a customer with no employer has no guarantor. */
    public static final String GUARANTOR_FIELD = "guarantor";
    /** The problem reported when a customer with no employer has no guarantor. */
    public static final String GUARANTOR_MESSAGE =
            "A customer with no employer needs a guarantor on the loan before it can be submitted for checking.";

    private static final long PAGE_SIZE = 100;
    private static final int MAX_PAGES = 1000;

    private final LoanGateway loans;
    private final LoanCoBorrowerGateway coBorrowers;
    private final LoanPaymentGateway payments;
    private final LoanValidator validator;
    private final CustomerProfileSource profiles;

    /**
     * @param profiles how the facts about a loan's customer are found, for the rules about
     *                 submitting a loan for checking
     * @throws NullPointerException if any argument is null
     */
    public LoanAccountService(LoanGateway loans, LoanCoBorrowerGateway coBorrowers,
                              LoanPaymentGateway payments, LoanValidator validator,
                              CustomerProfileSource profiles) {
        this.loans = Objects.requireNonNull(loans, "loans must not be null");
        this.coBorrowers = Objects.requireNonNull(coBorrowers, "coBorrowers must not be null");
        this.payments = Objects.requireNonNull(payments, "payments must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.profiles = Objects.requireNonNull(profiles, "profiles must not be null");
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

    /**
     * The loan's schedule with each repayment marked paid, due or upcoming, and the totals
     * worked out from the paid ones. Needs the mainframe: it reads the schedule and how many
     * payments have been recorded.
     *
     * @param loan the loan, as {@link #find} returned it
     */
    public RepaymentProgress progressOf(LoanRecord loan) {
        Objects.requireNonNull(loan, "loan must not be null");
        List<RepaymentEntry> schedule = scheduleOf(loan.loanNumber());
        return progress(loan.principal(), schedule, paymentsMade(loan.loanNumber()));
    }

    /**
     * Marks the repayments of a schedule and adds up what has been paid.
     *
     * <p>The mainframe records payments but does not say which repayment each one was
     * for, so the first {@code paymentsRecorded} repayments, in schedule order, count as
     * paid. The next one is due and the rest are upcoming. The amounts and dates of the
     * payments are not compared with the schedule.
     *
     * @param principal the amount borrowed; null counts as zero
     * @param schedule the repayments in payment order
     * @param paymentsRecorded how many payments the mainframe has recorded
     */
    public static RepaymentProgress progress(BigDecimal principal, List<RepaymentEntry> schedule,
                                             long paymentsRecorded) {
        Objects.requireNonNull(schedule, "schedule must not be null");
        int paid = (int) Math.max(0, Math.min(schedule.size(), paymentsRecorded));
        BigDecimal principalPaid = BigDecimal.ZERO;
        BigDecimal interestPaid = BigDecimal.ZERO;
        List<RepaymentProgress.Repayment> repayments = new ArrayList<>(schedule.size());
        for (int i = 0; i < schedule.size(); i++) {
            RepaymentEntry entry = schedule.get(i);
            RepaymentStatus status = i < paid ? RepaymentStatus.PAID
                    : i == paid ? RepaymentStatus.DUE : RepaymentStatus.UPCOMING;
            if (status == RepaymentStatus.PAID) {
                principalPaid = principalPaid.add(entry.principalAmount());
                interestPaid = interestPaid.add(entry.interestAmount());
            }
            repayments.add(new RepaymentProgress.Repayment(entry, status));
        }
        BigDecimal borrowed = principal == null ? BigDecimal.ZERO : principal;
        BigDecimal outstanding = borrowed.subtract(principalPaid).max(BigDecimal.ZERO);
        return new RepaymentProgress(repayments, paid, principalPaid, interestPaid, outstanding,
                paid < schedule.size() ? schedule.get(paid).date() : null);
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
     * Moves a loan from New to Pending, so it goes to be checked. Every rule is checked
     * first and every problem is reported at once: the loan must be New with all its
     * details present and valid, its customer must have a mailing address and a phone or
     * an email, and a customer with no employer needs a guarantor on the loan.
     *
     * @param loanNumber the mainframe's number for the loan
     * @return the loan as the mainframe now holds it
     * @throws LoanRuleException if the loan or its customer cannot be found, or a rule is broken
     * @throws MainframeUnavailableException if the mainframe could not be reached
     * @throws lavs.business.repository.RecordRejectedException if the mainframe refused the change
     */
    public LoanRecord submitForChecking(String loanNumber) {
        requireText(loanNumber, "loanNumber");
        LoanRecord loan = find(loanNumber).orElseThrow(() ->
                new LoanRuleException(Map.of(LoanRuleException.GENERAL, LoanService.LOAN_NOT_FOUND_MESSAGE)));
        if (loan.customerNumber() == null || loan.customerNumber().isBlank()) {
            throw new LoanRuleException(Map.of(LoanRuleException.GENERAL, LoanService.CUSTOMER_NOT_FOUND_MESSAGE));
        }
        CustomerProfile owner = offline(() -> profiles.profileOf(CustomerReference.existing(loan.customerNumber())))
                .orElseThrow(() -> new LoanRuleException(
                        Map.of(LoanRuleException.GENERAL, LoanService.CUSTOMER_NOT_FOUND_MESSAGE)));

        Map<String, String> problems = new LinkedHashMap<>();
        if (!NEW_STATUS.equalsIgnoreCase(loan.status())) {
            problems.put(STATUS_FIELD, STATUS_MESSAGE);
        }
        Optional<LoanDetails> terms = termsOf(loan);
        if (terms.isEmpty()) {
            problems.put(DETAILS_FIELD, DETAILS_MESSAGE);
        } else {
            validator.validate(terms.get()).forEach(problems::putIfAbsent);
        }
        if (!owner.hasMailingAddress()) {
            problems.put(LoanService.MAILING_ADDRESS_FIELD, LoanService.MAILING_ADDRESS_MESSAGE);
        }
        if (!owner.hasPhoneOrEmail()) {
            problems.put(LoanService.CONTACT_METHOD_FIELD, LoanService.CONTACT_METHOD_MESSAGE);
        }
        if (!owner.hasEmployer() && coBorrowersOf(loanNumber).stream()
                .noneMatch(person -> person.type() == CoBorrowerType.GUARANTOR)) {
            problems.put(GUARANTOR_FIELD, GUARANTOR_MESSAGE);
        }
        if (!problems.isEmpty()) {
            throw new LoanRuleException(problems);
        }

        offline(() -> {
            loans.submitForChecking(loanNumber);
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
