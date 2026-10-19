package lavs.ui.stub;

import lavs.business.repository.AuthGateway;
import lavs.business.repository.InvalidCredentialsException;

/**
 * Stand-in for the mainframe login until the real AuthGateway adapter exists.
 * Blocking on purpose, so callers must keep it off the JavaFX thread.
 */
public class StubAuthGateway implements AuthGateway {

    private static final String DEMO_EMPLOYEE_ID = "admin";
    private static final String DEMO_PASSWORD = "admin";

    private final long simulatedLatencyMillis;

    public StubAuthGateway() {
        this(700);
    }

    public StubAuthGateway(long simulatedLatencyMillis) {
        this.simulatedLatencyMillis = simulatedLatencyMillis;
    }

    @Override
    public String login(String username, String password) {
        pause();
        if (DEMO_EMPLOYEE_ID.equals(username) && DEMO_PASSWORD.equals(password)) {
            return username;
        }
        throw new InvalidCredentialsException("Rejected by the stub");
    }

    @Override
    public void logout() {
        pause();
    }

    private void pause() {
        try {
            Thread.sleep(simulatedLatencyMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
