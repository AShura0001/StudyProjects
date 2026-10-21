package lavs.infrastructure.mainframe;

import lavs.business.model.Address;
import lavs.business.model.CoBorrowerType;
import lavs.business.model.Customer;
import lavs.business.model.CustomerPayloads;
import lavs.business.model.CustomerPayloads.CustomerContent;
import lavs.business.model.CustomerReference;
import lavs.business.model.CustomerReference.ExistingCustomer;
import lavs.business.model.Draft;
import lavs.business.model.Email;
import lavs.business.model.Employer;
import lavs.business.model.LoanDetails;
import lavs.business.model.LoanPayloads;
import lavs.business.model.LoanPayloads.CoBorrowerContent;
import lavs.business.model.LoanPayloads.LoanContent;
import lavs.business.model.PhoneNumber;
import lavs.business.repository.DraftUploadGateway;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.RecordRejectedException;
import lavs.business.repository.UploadOutcomeUnknownException;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import mainframe.client.messages.customer.AddAddress;
import mainframe.client.messages.customer.AddEmail;
import mainframe.client.messages.customer.AddEmployer;
import mainframe.client.messages.customer.AddPhoneNumber;
import mainframe.client.messages.loan.AddCoborrower;
import mainframe.client.messages.loan.Create;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Sends saved drafts to the mainframe, for {@link lavs.business.service.DraftSyncService}.
 *
 * <p>It tells the three kinds of failure apart, as {@link DraftUploadGateway}
 * requires. It asks the connection whether the mainframe can be reached
 * <em>before</em> it sends anything, so a failure at that point is known to have
 * sent nothing. If that check passes and the send then fails without a usable
 * answer, the record may have been saved, and that is reported as an unknown
 * outcome. When the mainframe says no, the draft is refused with its reason.
 *
 * <p>Loan and co-borrower drafts are supported. Customer drafts are refused with a
 * clear message, because nothing has defined what a saved customer looks like yet.
 * A draft that cannot be read, or that came without the numbers it needs, is also
 * refused, without contacting the mainframe.
 */
public class MainframeDraftUploader implements DraftUploadGateway {


    /** The reason given when the text saved in a draft cannot be read. */
    public static final String UNREADABLE_DRAFT_MESSAGE =
            "The saved record could not be read, so it was not sent.";

    /** The reason given when a draft arrives without the mainframe numbers it needs. */
    public static final String MISSING_PARENT_MESSAGE =
            "The record belongs to another record that has no mainframe number yet, so it was not sent.";

    /** The message used when the mainframe cannot be reached before sending. */
    public static final String UNREACHABLE_MESSAGE = "The mainframe could not be reached.";

    private static final String UNREADABLE_REPLY_MESSAGE =
            "The mainframe's reply could not be read, so it is not known whether the record was saved.";

    private final Connection connection;

    /**
     * Creates an uploader that sends through the given connection, which must already be logged in.
     *
     * @param connection the connection to the mainframe
     * @throws NullPointerException if the connection is null
     */
    public MainframeDraftUploader(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
    }

    /** The checkpoint holding a new customer's number once customer.create has succeeded. */
    public static final String CUSTOMER_STEP = "customer";

    @Override
    public String upload(Draft draft, List<String> parentNumbers) {
        return upload(draft, parentNumbers, () -> { });
    }

    @Override
    public String upload(Draft draft, List<String> parentNumbers, Runnable saveProgress) {
        Objects.requireNonNull(draft, "draft must not be null");
        Objects.requireNonNull(parentNumbers, "parentNumbers must not be null");
        Objects.requireNonNull(saveProgress, "saveProgress must not be null");
        return switch (draft.getType()) {
            case LOAN -> uploadLoan(draft, parentNumbers);
            case COBORROWER -> uploadCoBorrower(draft, parentNumbers);
            case CUSTOMER -> uploadCustomer(draft, saveProgress);
        };
    }

    private String uploadLoan(Draft draft, List<String> parentNumbers) {
        LoanContent content;
        try {
            content = LoanPayloads.readLoan(draft.getPayload());
        } catch (IllegalArgumentException e) {
            throw refused(UNREADABLE_DRAFT_MESSAGE);
        }
        String customerNumber = numberOf(content.customer(), parentNumbers, 0);
        LoanDetails details = content.details();
        mainframe.client.messages.loan.Create message = new mainframe.client.messages.loan.Create(customerNumber,
                details.paymentAmount().doubleValue(), details.paymentFrequency(),
                details.principal().doubleValue(), details.rateAmount().doubleValue(),
                details.rateFrequency(), details.rateType(),
                details.renewalDate().toString(), details.startDate().toString());
        send(message);
        return requireNumber(message.getLoanNumber());
    }

    private String uploadCoBorrower(Draft draft, List<String> parentNumbers) {
        CoBorrowerContent content;
        try {
            content = LoanPayloads.readCoBorrower(draft.getPayload());
        } catch (IllegalArgumentException e) {
            throw refused(UNREADABLE_DRAFT_MESSAGE);
        }
        if (parentNumbers.isEmpty()) {
            throw refused(MISSING_PARENT_MESSAGE);
        }
        String loanNumber = parentNumbers.get(0);
        String coBorrowerNumber = numberOf(content.customer(), parentNumbers, 1);
        AddCoborrower message = new AddCoborrower(coBorrowerNumber, loanNumber, typeName(content.type()));
        send(message);
        return requireNumber(String.valueOf(message.getId()));
    }

    /**
     * A new customer takes several messages: customer.create, then one per address,
     * phone, email and employer. Each one that succeeds is recorded as a checkpoint and
     * saved before the next is sent, so a retry after a lost connection carries on from
     * there instead of creating the customer a second time.
     */
    private String uploadCustomer(Draft draft, Runnable saveProgress) {
        CustomerContent content;
        try {
            content = CustomerPayloads.readCustomer(draft.getPayload());
        } catch (IllegalArgumentException e) {
            throw refused(UNREADABLE_DRAFT_MESSAGE);
        }
        String customerNumber = draft.checkpoint(CUSTOMER_STEP).orElse(null);
        if (customerNumber == null) {
            Customer customer = content.customer();
            mainframe.client.messages.customer.Create create = new mainframe.client.messages.customer.Create(
                    customer.get_dateOfBirth(), customer.get_familyName(),
                    customer.get_firstName(), customer.get_nationality(), customer.getOrganisation(),
                    customer.get_title());
            send(create);
            customerNumber = requireNumber(create.getNumber());
            done(draft, saveProgress, CUSTOMER_STEP, customerNumber);
        }
        String number = customerNumber;
        int i = 0;
        for (Address a : content.addresses()) {
            sendOnce(draft, saveProgress, "address." + i++, number, "address " + i, () -> new AddAddress(
                    a.getCity(), a.getCountry(), number, a.getIsMailing(), a.getIsPrimary(), a.getLine1(),
                    a.getLine2(), a.getOtherType(), a.getPostCode(), a.getSuburb(), a.getType()));
        }
        i = 0;
        for (PhoneNumber p : content.phoneNumbers()) {
            sendOnce(draft, saveProgress, "phone." + i++, number, "phone number " + i, () -> new AddPhoneNumber(
                    p.is_canTxt(), p.get_countryCode(), number, p.is_isPrimary(), p.get_number(),
                    p.get_otherType(), p.get_type()));
        }
        i = 0;
        for (Email e : content.emails()) {
            sendOnce(draft, saveProgress, "email." + i++, number, "email " + i, () -> new AddEmail(
                    e.getAddress(), number, e.isPrimary(), e.getOtherType(), e.getType()));
        }
        i = 0;
        for (Employer emp : content.employers()) {
            sendOnce(draft, saveProgress, "employer." + i++, number, "employer " + i, () -> new AddEmployer(
                    number, emp.get_name(), emp.get_position(), emp.get_salary(), emp.get_type()));
        }
        return customerNumber;
    }

    /** Sends one part of a new customer unless an earlier attempt already did. */
    private void sendOnce(Draft draft, Runnable saveProgress, String step, String customerNumber, String what,
                          java.util.function.Supplier<Message> message) {
        if (draft.checkpoint(step).isPresent()) {
            return;
        }
        try {
            send(message.get());
        } catch (RecordRejectedException e) {
            String reason = e.getMessage() == null || e.getMessage().isBlank() ? "no reason given" : e.getMessage();
            throw new RecordRejectedException("Customer " + customerNumber + " was created, but the mainframe refused "
                    + what + ": " + reason, e.getFieldErrors());
        }
        done(draft, saveProgress, step, "sent");
    }

    private static void done(Draft draft, Runnable saveProgress, String step, String value) {
        draft.recordCheckpoint(step, value);
        saveProgress.run();
    }

    private static String numberOf(CustomerReference customer, List<String> parentNumbers, int position) {
        if (customer instanceof ExistingCustomer existing) {
            return existing.customerNumber();
        }
        if (parentNumbers.size() <= position) {
            throw refused(MISSING_PARENT_MESSAGE);
        }
        return parentNumbers.get(position);
    }

    private static String typeName(CoBorrowerType type) {
        return type == CoBorrowerType.GUARANTOR ? AddCoborrower.TYPE_GUARANTOR : AddCoborrower.TYPE_COBORROWER;
    }

    private void send(Message message) {
        if (!connection.isConnected()) {
            throw new MainframeUnavailableException(UNREACHABLE_MESSAGE);
        }
        Status status;
        try {
            status = message.send(connection);
        } catch (IOException | RuntimeException e) {
            throw new UploadOutcomeUnknownException(UNREADABLE_REPLY_MESSAGE, e);
        }
        if (status.getWasSuccessful()) {
            return;
        }
        if (status.getErrorCode() == StatusMessage.SERVER_FAILURE.getCode()) {
            throw new RecordRejectedException(status.getErrorMessage(), Map.of());
        }
        throw new UploadOutcomeUnknownException(UNREADABLE_REPLY_MESSAGE);
    }

    private static String requireNumber(String number) {
        if (number == null || number.isBlank() || "null".equals(number)) {
            throw new UploadOutcomeUnknownException(UNREADABLE_REPLY_MESSAGE);
        }
        return number;
    }

    private static RecordRejectedException refused(String reason) {
        return new RecordRejectedException(reason, Map.of());
    }
}
