package lavs.ui.service;

import lavs.business.repository.MainframeUnavailableException;
import mainframe.client.Connection;
import mainframe.client.Status;
import mainframe.client.messages.loan.List;
import mainframe.client.messages.loan.Retrieve;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;

/**
 * TEMPORARY. The mainframe can list a customer's loans and give a loan's rate,
 * payment and dates, but no gateway in the business layer exposes either yet.
 * Until the backend adds them, this reads those two messages directly. When the
 * gateways exist, delete this class and call them instead.
 */
final class MainframeLoanLookup {

    private static final long LIST_SIZE = 100;

    /** What the mainframe says about one loan, with the raw text kept as sent. */
    record Facts(String loanNumber, double principal, double rateAmount, String rateType,
                 String rateFrequency, double paymentAmount, String paymentFrequency,
                 LocalDate startDate, LocalDate renewalDate, String status) {
    }

    private final Connection connection;

    MainframeLoanLookup(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
    }

    java.util.List<CustomerDirectory.Loan> loansOf(String customerNumber) {
        java.util.List<CustomerDirectory.Loan> loans = new ArrayList<>();
        List message = new List(customerNumber, 1, LIST_SIZE);
        Status status = send(message);
        if (!status.getWasSuccessful() || message.getItems() == null) {
            return loans;
        }
        for (List.Item item : message.getItems()) {
            String number = item.getLoanNumber();
            String suffix = number.contains("-") ? number.substring(number.lastIndexOf('-') + 1) : number;
            loans.add(new CustomerDirectory.Loan("Loan " + suffix, dateText(item.getStartDate()), number));
        }
        return loans;
    }

    /** @return the loan, or null when the mainframe does not know the number */
    Facts facts(String loanNumber) {
        Retrieve message = new Retrieve(loanNumber);
        Status status = send(message);
        if (!status.getWasSuccessful() || message.getLoanNumber() == null) {
            return null;
        }
        return new Facts(message.getLoanNumber(), nz(message.getPrincipal()), nz(message.getRateAmount()),
                message.getRateType(), message.getRateFrequency(), nz(message.getPaymentAmount()),
                message.getPaymentFrequency(), date(message.getStartDate()), date(message.getRenewalDate()),
                message.getStatus());
    }

    private Status send(mainframe.client.Message message) {
        try {
            return message.send(connection);
        } catch (IOException e) {
            throw new MainframeUnavailableException("The mainframe could not be reached.", e);
        }
    }

    private static double nz(Double value) {
        return value == null ? 0 : value;
    }

    private static LocalDate date(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDate.parse(text.length() > 10 ? text.substring(0, 10) : text);
    }

    private static String dateText(String text) {
        return text == null ? "" : (text.length() > 10 ? text.substring(0, 10) : text);
    }
}
