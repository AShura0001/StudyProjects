package lavs.infrastructure.mainframe;

import lavs.business.repository.PageResult;
import mainframe.client.Connection;
import mainframe.client.HttpConnection;
import mainframe.client.Status;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class MainframeIntegrationTestSupport {

    protected Connection connection;

    protected final String baseUrl = System.getProperty(
            "mainframe.url",
            "https://animating-eraser-arrogance.ngrok-free.dev/"
    );

    protected final String username = System.getProperty(
            "mainframe.username",
            "Group_2"
    );

    protected final String password = System.getProperty(
            "mainframe.password",
            "bug"
    );

    /** How many times to try connecting, logging in and sending queries. */
    private static final int MAX_ATTEMPTS =
            Integer.getInteger("mainframe.retry.attempts", 4);

    /** First wait between attempts; doubles after each failed attempt. */
    private static final long INITIAL_RETRY_DELAY_MILLIS =
            Long.getLong("mainframe.retry.delayMillis", 500L);

    /**
     * Connects and logs in once for the whole test class (the class is
     * PER_CLASS), retrying with backoff so a brief tunnel drop-out doesn't
     * fail the run.
     */
    @BeforeAll
    void connectToMainframe() throws Exception {
        HttpConnection http = new HttpConnection(baseUrl);
        connection = new RetryingConnection(http, MAX_ATTEMPTS, INITIAL_RETRY_DELAY_MILLIS);

        long delay = INITIAL_RETRY_DELAY_MILLIS;
        boolean reachable = connection.isConnected();
        for (int attempt = 2; attempt <= MAX_ATTEMPTS && !reachable; attempt++) {
            RetryingConnection.sleep(delay);
            delay *= 2;
            reachable = connection.isConnected();
        }
        assertTrue(
                reachable,
                "The mainframe is not reachable after " + MAX_ATTEMPTS
                        + " attempts: " + baseUrl
        );

        // Only retry logins that failed on the network; bad credentials fail at once.
        delay = INITIAL_RETRY_DELAY_MILLIS;
        Status loginStatus = connection.Login(username, password);
        for (int attempt = 2;
             attempt <= MAX_ATTEMPTS && RetryingConnection.isNetworkFailure(loginStatus);
             attempt++) {
            RetryingConnection.sleep(delay);
            delay *= 2;
            loginStatus = connection.Login(username, password);
        }
        assertTrue(
                loginStatus.getWasSuccessful(),
                "Mainframe login failed: "
                        + loginStatus.getErrorMessage()
        );
    }

    @AfterAll
    void closeMainframeConnection() throws Exception {
        if (connection != null) {
            connection.Logout();
            connection.close();
        }
    }

    @FunctionalInterface
    protected interface PageFetcher<T> {
        PageResult<T> fetch(long page, long pageSize) throws IOException;
    }

    /**
     * Walks every page of a list call and returns all items, so tests can
     * find records they created even when the customer has many of them.
     */
    protected static <T> List<T> fetchAllPages(PageFetcher<T> fetcher)
            throws IOException {
        final long pageSize = 20;
        final int maxPages = 100;
        List<T> all = new ArrayList<>();

        for (long page = 1; page <= maxPages; page++) {
            PageResult<T> result = fetcher.fetch(page, pageSize);
            all.addAll(result.getList());
            if (result.getList().isEmpty() || all.size() >= result.getTotal()) {
                break;
            }
        }
        return all;
    }

    /**
     * Creates a new loan in the New status for a customer. The mainframe cannot
     * delete loans, so only call this when a test needs a fresh one.
     */
    protected String createTestLoan(String customerNumber) throws IOException {
        mainframe.client.messages.loan.Create create = new mainframe.client.messages.loan.Create(
                customerNumber,
                100.00,
                mainframe.client.messages.loan.Create.PAYMENTFREQUENCY_MONTHLY,
                5000.00,
                5.0,
                mainframe.client.messages.loan.Create.RATEFREQUENCY_YEARLY,
                mainframe.client.messages.loan.Create.RATETYPE_FIXED,
                "2031-01-01",
                "2026-01-01");
        Status status = create.send(connection);
        assertTrue(status.getWasSuccessful(), "Could not create a test loan: " + status.getErrorMessage());
        return create.getLoanNumber();
    }

    /**
     * Reuses one of the customer's loans that is still New, or creates one, so
     * test runs don't keep adding loans.
     */
    protected String findOrCreateTestLoan(String customerNumber) throws IOException {
        mainframe.client.messages.loan.List loans = new mainframe.client.messages.loan.List(customerNumber, 1, 50);
        if (loans.send(connection).getWasSuccessful()) {
            for (mainframe.client.messages.loan.List.Item loan : loans.getItems()) {
                if (mainframe.client.messages.loan.UpdateStatus.STATUS_NEW.equalsIgnoreCase(loan.getStatus())) {
                    return loan.getLoanNumber();
                }
            }
        }
        return createTestLoan(customerNumber);
    }

    /**
     * Finds an existing customer other than the given one, for records that link
     * two customers. Set -Dmainframe.secondaryCustomerNumber to choose one.
     */
    protected String findOtherCustomerNumber(String excluding) throws IOException {
        String configured = System.getProperty("mainframe.secondaryCustomerNumber");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        mainframe.client.messages.customer.Search search =
                new mainframe.client.messages.customer.Search(1, "Smith", 25);
        assertTrue(search.send(connection).getWasSuccessful(), "Customer search failed");

        for (mainframe.client.messages.customer.Search.Item item : search.getItems()) {
            if (!excluding.equals(item.getNumber())) {
                return item.getNumber();
            }
        }
        throw new AssertionError("No second customer found; set -Dmainframe.secondaryCustomerNumber");
    }
}