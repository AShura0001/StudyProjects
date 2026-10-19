package lavs.infrastructure.mainframe;

import lavs.business.model.CoBorrowerType;
import lavs.business.model.CustomerReference;
import lavs.business.model.CustomerReference.ExistingCustomer;
import lavs.business.model.Draft;
import lavs.business.model.LoanDetails;
import lavs.business.model.LoanPayloads;
import lavs.business.model.LoanPayloads.CoBorrowerContent;
import lavs.business.model.LoanPayloads.LoanContent;
import lavs.business.repository.DraftUploadGateway;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.RecordRejectedException;
import lavs.business.repository.UploadOutcomeUnknownException;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
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

    /** The reason given when a customer draft is sent. */
    public static final String CUSTOMER_NOT_SUPPORTED_MESSAGE =
            "Sending a new customer to the mainframe is not supported yet.";

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

    @Override
    public String upload(Draft draft, List<String> parentNumbers) {
        Objects.requireNonNull(draft, "draft must not be null");
        Objects.requireNonNull(parentNumbers, "parentNumbers must not be null");
        return switch (draft.getType()) {
            case LOAN -> uploadLoan(draft, parentNumbers);
            case COBORROWER -> uploadCoBorrower(draft, parentNumbers);
            case CUSTOMER -> throw refused(CUSTOMER_NOT_SUPPORTED_MESSAGE);
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
        Create message = new Create(customerNumber,
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
