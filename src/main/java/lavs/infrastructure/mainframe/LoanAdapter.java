package lavs.infrastructure.mainframe;

import lavs.business.model.LoanSummary;
import lavs.business.model.RepaymentEntry;
import lavs.business.repository.LoanGateway;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.RecordRejectedException;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import mainframe.client.messages.loan.UpdateStatus;
import mainframe.client.messages.loanSummary.Retrieve;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Asks the mainframe for the figures it works out for a loan, and moves a loan to
 * Pending, for {@link lavs.business.service.LoanService}.
 *
 * <p>When the mainframe says no, a lookup gives an empty result and a status change
 * is refused with the reason. Any other trouble, such as the mainframe not being
 * reachable or its reply not being readable, is reported as
 * {@link MainframeUnavailableException}.
 */
public class LoanAdapter implements LoanGateway {

    /** The message used when the mainframe cannot be reached or its reply cannot be read. */
    public static final String UNAVAILABLE_MESSAGE =
            "The mainframe could not be reached or its reply could not be read.";

    private static final long PAGE_SIZE = 100;
    private static final int MAX_PAGES = 1000;
    private static final int DATE_LENGTH = "yyyy-MM-dd".length();

    private final Connection connection;

    /**
     * Creates an adapter that asks through the given connection, which must already be logged in.
     *
     * @param connection the connection to the mainframe
     * @throws NullPointerException if the connection is null
     */
    public LoanAdapter(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
    }

    @Override
    public Optional<LoanSummary> findSummary(String loanNumber) {
        Retrieve message = new Retrieve(Objects.requireNonNull(loanNumber, "loanNumber must not be null"));
        Status status = send(message);
        if (!status.getWasSuccessful()) {
            requireNotFound(status);
            return Optional.empty();
        }
        try {
            return Optional.of(new LoanSummary(
                    message.getLoanNumber(),
                    message.getCustomerName(),
                    message.getNumberOfPayments(),
                    BigDecimal.valueOf(message.getTotalAmountPaid()),
                    BigDecimal.valueOf(message.getTotalInterestPaid()),
                    dateOf(message.getFinalPaymentDate()),
                    BigDecimal.valueOf(message.getFinalPaymentAmount())));
        } catch (RuntimeException e) {
            throw new MainframeUnavailableException(UNAVAILABLE_MESSAGE, e);
        }
    }

    @Override
    public List<RepaymentEntry> findRepaymentSchedule(String loanNumber) {
        Objects.requireNonNull(loanNumber, "loanNumber must not be null");
        List<RepaymentEntry> schedule = new ArrayList<>();
        for (long page = 1; page <= MAX_PAGES; page++) {
            mainframe.client.messages.loanRepaymentSchedule.List message =
                    new mainframe.client.messages.loanRepaymentSchedule.List(loanNumber, page, PAGE_SIZE);
            Status status = send(message);
            if (!status.getWasSuccessful()) {
                requireNotFound(status);
                return List.of();
            }
            try {
                var items = message.getItems();
                for (var item : items) {
                    schedule.add(new RepaymentEntry(item.getPaymentNumber(), dateOf(item.getDate()),
                            BigDecimal.valueOf(item.getAmountPaid()), BigDecimal.valueOf(item.getPrincipalAmount()),
                            BigDecimal.valueOf(item.getInterestAmount()), BigDecimal.valueOf(item.getAmountRemaining())));
                }
                if (items.length == 0 || schedule.size() >= message.getTotal()) {
                    return schedule;
                }
            } catch (RuntimeException e) {
                throw new MainframeUnavailableException(UNAVAILABLE_MESSAGE, e);
            }
        }
        return schedule;
    }

    @Override
    public void submitForChecking(String loanNumber) {
        UpdateStatus message = new UpdateStatus(
                Objects.requireNonNull(loanNumber, "loanNumber must not be null"), UpdateStatus.STATUS_PENDING);
        Status status = send(message);
        if (status.getWasSuccessful()) {
            return;
        }
        if (status.getErrorCode() == StatusMessage.SERVER_FAILURE.getCode()) {
            throw new RecordRejectedException(status.getErrorMessage(), Map.of());
        }
        throw new MainframeUnavailableException(UNAVAILABLE_MESSAGE);
    }

    private Status send(Message message) {
        try {
            return message.send(connection);
        } catch (IOException | RuntimeException e) {
            throw new MainframeUnavailableException(UNAVAILABLE_MESSAGE, e);
        }
    }

    /** Passes quietly when the mainframe simply said no, and fails when something else went wrong. */
    private static void requireNotFound(Status status) {
        if (status.getErrorCode() != StatusMessage.SERVER_FAILURE.getCode()) {
            throw new MainframeUnavailableException(UNAVAILABLE_MESSAGE);
        }
    }

    private static LocalDate dateOf(String text) {
        return LocalDate.parse(text.length() > DATE_LENGTH ? text.substring(0, DATE_LENGTH) : text);
    }
}
