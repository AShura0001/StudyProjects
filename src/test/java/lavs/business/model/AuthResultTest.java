package lavs.business.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthResultTest {

    @Test
    void ok_whenCreated_isSuccessfulWithNoMessage() {
        AuthResult result = AuthResult.ok();

        assertTrue(result.success());
        assertNull(result.message());
    }

    @Test
    void failure_whenCreated_isUnsuccessfulWithMessage() {
        AuthResult result = AuthResult.failure("Employee ID or password is incorrect.");

        assertFalse(result.success());
        assertEquals("Employee ID or password is incorrect.", result.message());
    }

    @Test
    void failure_withNullMessage_isRejected() {
        assertThrows(NullPointerException.class, () -> AuthResult.failure(null));
    }
}
