package lavs.business.repository;

/**
 * What {@link lavs.business.service.AuthService} needs from the mainframe to log
 * staff in and out.
 *
 * <p>The business layer only knows about this interface, so tests can use a mock
 * and the data access layer decides how the mainframe is actually called.
 */
public interface AuthGateway {

    /**
     * Logs in to the mainframe. On success, later mainframe calls are made as
     * this user until {@link #logout()} is called.
     *
     * @param username the staff member's login name
     * @param password the staff member's password
     * @return the username as the mainframe confirmed it
     * @throws InvalidCredentialsException if the mainframe refused the username or password
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    String login(String username, String password);

    /**
     * Ends the current mainframe session. Does nothing if no one is logged in.
     */
    void logout();
}
