package lavs.infrastructure.mainframe;

import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.RecordRejectedException;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

/**
 * Shared helpers for adapters that report mainframe problems with the business
 * layer's exceptions instead of status codes.
 */
final class MainframeCalls {

    static final String UNREACHABLE_MESSAGE = "The mainframe could not be reached.";
    static final String REJECTED_MESSAGE = "The mainframe refused the request.";

    private MainframeCalls() {
    }

    /**
     * Sends a message.
     *
     * @return the status; it may be unsuccessful if the mainframe refused the message
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    static Status send(Message message, Connection connection) {
        Status status;
        try {
            status = message.send(connection);
        } catch (IOException e) {
            throw new MainframeUnavailableException(UNREACHABLE_MESSAGE, e);
        }
        requireSession(status);
        if (isNetworkFailure(status)) {
            throw new MainframeUnavailableException(UNREACHABLE_MESSAGE);
        }
        return status;
    }

    /**
     * Fails when the mainframe no longer accepts this login session, so callers never
     * mistake it for a refused record or an unknown outcome.
     *
     * @throws lavs.business.repository.SessionExpiredException if the session has ended
     */
    static void requireSession(Status status) {
        if (Objects.equals(status.getErrorCode(), mainframe.client.StatusMessage.SESSION_EXPIRED.getCode())) {
            throw new lavs.business.repository.SessionExpiredException();
        }
    }

    /**
     * Sends a message that changes a record.
     *
     * @throws RecordRejectedException if the mainframe refused the message
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    static void sendChange(Message message, Connection connection) {
        Status status = send(message, connection);
        if (!status.getWasSuccessful()) {
            String reason = status.getErrorMessage();
            throw new RecordRejectedException(
                    reason == null || reason.isBlank() ? REJECTED_MESSAGE : reason,
                    Map.of());
        }
    }

    static boolean isNetworkFailure(Status status) {
        return !status.getWasSuccessful()
                && StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode().equals(status.getErrorCode());
    }

    /** Turns a mainframe amount into money, or null if it was not sent. */
    static BigDecimal money(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    /**
     * Reads a mainframe date such as {@code 2025-12-24}. Any time part after the
     * date is ignored.
     *
     * @return the date, or null if it was not sent
     */
    static LocalDate date(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
    }

    /** Fails if a value the record cannot be built without was not sent. */
    static <T> T required(T value, String name) {
        if (value == null) {
            throw new IllegalStateException("The mainframe did not send " + name + ".");
        }
        return value;
    }
}
