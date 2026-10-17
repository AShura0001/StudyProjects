package lavs.infrastructure.mainframe;

import mainframe.client.Connection;
import mainframe.client.HttpConnection;
import mainframe.client.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

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
            "admin"
    );

    protected final String password = System.getProperty(
            "mainframe.password",
            "admin"
    );

    @BeforeEach
    void connectToMainframe() throws Exception {
        connection = new HttpConnection(baseUrl);

        assertTrue(
                connection.isConnected(),
                "The mainframe is not reachable: " + baseUrl
        );

        Status loginStatus =
                connection.Login(username, password);

        assertTrue(
                loginStatus.getWasSuccessful(),
                "Mainframe login failed: "
                        + loginStatus.getErrorMessage()
        );
    }

    @AfterEach
    void closeMainframeConnection() throws Exception {
        if (connection != null) {
            connection.Logout();
            connection.close();
        }
    }
}