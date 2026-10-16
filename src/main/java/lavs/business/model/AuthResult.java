package lavs.business.model;

import java.util.Objects;

/**
 * The outcome of a login attempt, in a form the login screen can show directly.
 *
 * @param success whether the login worked
 * @param message why the login failed, or null when it worked
 */
public record AuthResult(boolean success, String message) {

    /**
     * Creates a result for a login that worked.
     *
     * @return a successful result with no message
     */
    public static AuthResult ok() {
        return new AuthResult(true, null);
    }

    /**
     * Creates a result for a login that failed.
     *
     * @param message why it failed, worded for the user
     * @return a failed result carrying the message
     * @throws NullPointerException if the message is null
     */
    public static AuthResult failure(String message) {
        return new AuthResult(false, Objects.requireNonNull(message, "message must not be null"));
    }
}
