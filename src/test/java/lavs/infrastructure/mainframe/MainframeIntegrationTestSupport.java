package lavs.infrastructure.mainframe;

import lavs.business.model.CustomerType;
import lavs.business.repository.CustomerSearchResult;
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
    protected interface PageFetcher<T extends CustomerType> {
        CustomerSearchResult<T> fetch(long page, long pageSize) throws IOException;
    }

    /**
     * Walks every page of a list call and returns all items, so tests can
     * find records they created even when the customer has many of them.
     */
    protected static <T extends CustomerType> List<T> fetchAllPages(PageFetcher<T> fetcher)
            throws IOException {
        final long pageSize = 20;
        final int maxPages = 100;
        List<T> all = new ArrayList<>();

        for (long page = 1; page <= maxPages; page++) {
            CustomerSearchResult<T> result = fetcher.fetch(page, pageSize);
            all.addAll(result.getList());
            if (result.getList().isEmpty() || all.size() >= result.getTotal()) {
                break;
            }
        }
        return all;
    }
}