package lavs.business.model;

import java.time.Instant;
import java.util.Objects;

/**
 * The staff member who is currently logged in to the mainframe.
 *
 * @param username the login name the mainframe accepted
 * @param loggedInAt the moment the login succeeded
 */
public record Session(String username, Instant loggedInAt) {

    /**
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the username is blank
     */
    public Session {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(loggedInAt, "loggedInAt must not be null");
        if (username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
    }
}
