package lavs.business.service;

import lavs.business.model.CoBorrowerType;
import lavs.business.model.CustomerProfile;
import lavs.business.model.CustomerReference;
import lavs.business.model.CustomerReference.DraftCustomer;
import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;
import lavs.business.model.DraftType;
import lavs.business.model.LoanDetails;
import lavs.business.model.LoanPayloads;
import lavs.business.model.LoanPayloads.LoanContent;
import lavs.business.model.LoanSummary;
import lavs.business.model.RepaymentEntry;
import lavs.business.repository.DraftRepository;
import lavs.business.repository.LoanGateway;
import lavs.business.repository.MainframeUnavailableException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * <p>The calls that contact the mainframe can take a while, so a screen must call
 * them off its own thread.
 */
public class LoanService {

    /** The message shown when the summary is asked for without a connection. */
    public static final String SUMMARY_OFFLINE_MESSAGE =
            "The loan summary needs a connection to the mainframe. Please try again when you are back online.";

    /** The message shown when the schedule is asked for without a connection. */
    public static final String SCHEDULE_OFFLINE_MESSAGE =
            "The repayment schedule needs a connection to the mainframe. Please try again when you are back online.";

    /** The key of the problem reported when the customer has no mailing address. */
    public static final String MAILING_ADDRESS_FIELD = "mailingAddress";

    /** The problem reported when the customer has no mailing address. */
    public static final String MAILING_ADDRESS_MESSAGE =
            "A loan cannot start until the customer has a mailing address.";

    /** The key of the problem reported when the customer has no phone number or email. */
    public static final String CONTACT_METHOD_FIELD = "contactMethod";

    /** The problem reported when the customer has no phone number or email. */
    public static final String CONTACT_METHOD_MESSAGE =
            "A loan cannot start until the customer has a phone number or an email.";

    /** The problem reported when the customer cannot be found. */
    public static final String CUSTOMER_NOT_FOUND_MESSAGE = "The customer could not be found.";

    /** The problem reported when the loan cannot be found. */
    public static final String LOAN_NOT_FOUND_MESSAGE = "The loan could not be found.";

    /** The key of the problem reported when a customer is made a co-borrower on their own loan. */
    public static final String CO_BORROWER_FIELD = "coBorrower";

    /** The problem reported when a customer is made a co-borrower on their own loan. */
    public static final String OWN_LOAN_MESSAGE = "A customer cannot be a co-borrower on their own loan.";

    private final LoanGateway gateway;
    private final CustomerProfileSource profiles;
    private final LoanValidator validator;
    private final BackgroundSync sync;
    private final DraftRepository drafts;

    /**
     * Creates the loan service.
     *
     * @param gateway how the mainframe is asked for the figures it works out
     * @param profiles how the facts about a customer are found
     * @param validator how the values of a loan are checked
     * @param sync saves drafts and sends them in the background
     * @param drafts where saved drafts are read from
     * @throws NullPointerException if any argument is null
     */
    public LoanService(LoanGateway gateway, CustomerProfileSource profiles, LoanValidator validator,
                       BackgroundSync sync, DraftRepository drafts) {
        this.gateway = Objects.requireNonNull(gateway, "gateway must not be null");
        this.profiles = Objects.requireNonNull(profiles, "profiles must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.sync = Objects.requireNonNull(sync, "sync must not be null");
        this.drafts = Objects.requireNonNull(drafts, "drafts must not be null");
    }

    /**
     * Saves a new loan as a draft and asks for it to be sent. This works without a
     * connection when the customer is a draft, or when the facts about an existing
     * customer can be found without the mainframe.
     *
     * <p>Every problem is reported at once: the customer needs a mailing address and
     * a phone number or an email, and the loan's values must pass the validator.
     *
     * @param customer the customer the loan is for, on the mainframe or still a draft
     * @param details the values of the loan
     * @return the draft that was saved
     * @throws NullPointerException if either argument is null
     * @throws LoanRuleException if a rule is broken, with every problem found
     * @throws MainframeUnavailableException if an existing customer had to be looked
     *                                       up on the mainframe and it could not be reached
     * @throws lavs.business.repository.DraftStorageException if the draft could not be saved
     */
    public Draft createLoan(CustomerReference customer, LoanDetails details) {
        Objects.requireNonNull(customer, "customer must not be null");
        Objects.requireNonNull(details, "details must not be null");
        CustomerProfile profile = profiles.profileOf(customer)
                .orElseThrow(() -> new LoanRuleException(Map.of(LoanRuleException.GENERAL, CUSTOMER_NOT_FOUND_MESSAGE)));
        Map<String, String> problems = new LinkedHashMap<>();
        if (!profile.hasMailingAddress()) {
            problems.put(MAILING_ADDRESS_FIELD, MAILING_ADDRESS_MESSAGE);
        }
        if (!profile.hasPhoneOrEmail()) {
            problems.put(CONTACT_METHOD_FIELD, CONTACT_METHOD_MESSAGE);
        }
        validator.validate(details).forEach(problems::putIfAbsent);
        if (!problems.isEmpty()) {
            throw new LoanRuleException(problems);
        }
        return sync.saveDraft(DraftType.LOAN, LoanPayloads.loanPayload(customer, details), dependenciesOf(customer));
    }

    /**
     * Saves a co-borrower or guarantor for a loan as a draft and asks for it to be
     * sent. It is sent only after the loan, and after the co-borrower's own customer
     * if that is still a draft.
     *
     * @param loanDraftId the local ID of the loan's draft
     * @param type whether the customer is a co-borrower or a guarantor
     * @param coBorrower the customer who is the co-borrower or guarantor
     * @return the draft that was saved
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the draft is not a loan, or the co-borrower
     *                                  is a draft that has not been saved
     * @throws LoanRuleException if the loan cannot be found, or the customer is the
     *                           loan's own customer
     * @throws lavs.business.repository.DraftStorageException if the draft could not be saved
     */
    public Draft addCoBorrower(String loanDraftId, CoBorrowerType type, CustomerReference coBorrower) {
        Objects.requireNonNull(loanDraftId, "loanDraftId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(coBorrower, "coBorrower must not be null");
        Draft loan = drafts.findById(loanDraftId)
                .orElseThrow(() -> new LoanRuleException(Map.of(LoanRuleException.GENERAL, LOAN_NOT_FOUND_MESSAGE)));
        if (loan.getType() != DraftType.LOAN) {
            throw new IllegalArgumentException("draft " + loanDraftId + " is not a loan");
        }
        LoanContent content = LoanPayloads.readLoan(loan.getPayload());
        if (content.customer().equals(coBorrower)) {
            throw new LoanRuleException(Map.of(CO_BORROWER_FIELD, OWN_LOAN_MESSAGE));
        }
        List<String> dependsOn = new ArrayList<>();
        dependsOn.add(loanDraftId);
        dependsOn.addAll(dependenciesOf(coBorrower));
        return sync.saveDraft(DraftType.COBORROWER, LoanPayloads.coBorrowerPayload(type, coBorrower), dependsOn);
    }

    /**
     * Fixes a refused (or still waiting) loan draft and queues it to be sent again.
     * The same rules as {@link #createLoan} are checked first.
     *
     * @param loanDraftId the local ID of the loan's draft
     * @param details the corrected values
     * @return the revised draft
     * @throws LoanRuleException if a rule is broken, or the draft is missing or not a loan
     * @throws IllegalStateException if the draft is being sent, uncertain or already sent
     */
    public Draft reviseLoan(String loanDraftId, LoanDetails details) {
        Objects.requireNonNull(loanDraftId, "loanDraftId must not be null");
        Objects.requireNonNull(details, "details must not be null");
        Draft draft = drafts.findById(loanDraftId)
                .filter(d -> d.getType() == DraftType.LOAN)
                .orElseThrow(() -> new LoanRuleException(Map.of(LoanRuleException.GENERAL, LOAN_NOT_FOUND_MESSAGE)));
        CustomerReference customer = LoanPayloads.readLoan(draft.getPayload()).customer();
        Map<String, String> problems = new LinkedHashMap<>(validator.validate(details));
        if (!problems.isEmpty()) {
            throw new LoanRuleException(problems);
        }
        return sync.revise(loanDraftId, LoanPayloads.loanPayload(customer, details));
    }

    /**
     * Loans for an existing customer that are saved here but not yet confirmed by the
     * mainframe: waiting to be sent, uncertain, or refused.
     *
     * @return the drafts, oldest first
     */
    public List<Draft> unsentLoansOf(String customerNumber) {
        Objects.requireNonNull(customerNumber, "customerNumber must not be null");
        CustomerReference owner = CustomerReference.existing(customerNumber);
        List<Draft> found = new ArrayList<>();
        for (Draft draft : sync.findAll()) {
            if (draft.getType() != DraftType.LOAN || draft.getStatus() == DraftStatus.SYNCED) {
                continue;
            }
            try {
                if (LoanPayloads.readLoan(draft.getPayload()).customer().equals(owner)) {
                    found.add(draft);
                }
            } catch (IllegalArgumentException unreadable) {
                // An unreadable draft is shown on the Drafts screen instead.
            }
        }
        return found;
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

    private static List<String> dependenciesOf(CustomerReference customer) {
        if (customer instanceof DraftCustomer draftCustomer) {
            return List.of(draftCustomer.draftId());
        }
        return List.of();
    }

    private static void requireLoanNumber(String loanNumber) {
        Objects.requireNonNull(loanNumber, "loanNumber must not be null");
        if (loanNumber.isBlank()) {
            throw new IllegalArgumentException("loanNumber must not be blank");
        }
    }
}
