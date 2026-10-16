package lavs.business.service;

import lavs.business.model.AuthResult;
import lavs.business.model.Session;
import lavs.business.repository.AuthGateway;
import lavs.business.repository.InvalidCredentialsException;
import lavs.business.repository.MainframeUnavailableException;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/**
 * Logs staff in and out through the mainframe and holds the current session.
 *
 * <p>Login blocks while the mainframe is called, so the UI must call it off the
 * JavaFX thread. The session may then be read from any thread.
 */
public class AuthService {

    static final String MISSING_DETAILS = "Enter your Employee ID and Password.";
    static final String INVALID_CREDENTIALS = "Employee ID or password is incorrect.";
    static final String UNAVAILABLE = "Could not reach the mainframe. Check your connection and try again.";

    private final AuthGateway gateway;
    private final Clock clock;
    private volatile Session session;

    /**
     * Creates a service that stamps sessions with the system clock.
     *
     * @param gateway how to reach the mainframe
     * @throws NullPointerException if the gateway is null
     */
    public AuthService(AuthGateway gateway) {
        this(gateway, Clock.systemUTC());
    }

    /**
     * Creates a service with a given clock, so tests can control login times.
     *
     * @param gateway how to reach the mainframe
     * @param clock where login times come from
     * @throws NullPointerException if any argument is null
     */
    public AuthService(AuthGateway gateway, Clock clock) {
        this.gateway = Objects.requireNonNull(gateway, "gateway must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Tries to log in. On success a new session starts, replacing any previous one.
     * On failure the previous session, if any, is left alone.
     *
     * @param employeeId the staff member's login name
     * @param password the staff member's password
     * @return a successful result, or a failed one with a message for the user
     */
    public AuthResult login(String employeeId, String password) {
        if (employeeId == null || employeeId.isBlank() || password == null || password.isEmpty()) {
            return AuthResult.failure(MISSING_DETAILS);
        }

        String confirmedUsername;
        try {
            confirmedUsername = gateway.login(employeeId.strip(), password);
        } catch (InvalidCredentialsException e) {
            return AuthResult.failure(INVALID_CREDENTIALS);
        } catch (MainframeUnavailableException e) {
            return AuthResult.failure(UNAVAILABLE);
        }

        String username = confirmedUsername == null || confirmedUsername.isBlank()
                ? employeeId.strip()
                : confirmedUsername;
        session = new Session(username, clock.instant());
        return AuthResult.ok();
    }

    /**
     * Ends the current session. Does nothing if no one is logged in.
     */
    public void logout() {
        if (session == null) {
            return;
        }
        gateway.logout();
        session = null;
    }

    /**
     * Gets the current session.
     *
     * @return the session, or empty if no one is logged in
     */
    public Optional<Session> currentSession() {
        return Optional.ofNullable(session);
    }

    /**
     * Checks whether someone is logged in.
     *
     * @return true if there is a current session
     */
    public boolean isLoggedIn() {
        return session != null;
    }
}
