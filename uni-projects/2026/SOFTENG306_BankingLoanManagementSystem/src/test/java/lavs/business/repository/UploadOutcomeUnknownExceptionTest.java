package lavs.business.repository;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class UploadOutcomeUnknownExceptionTest {

    @Test
    void newException_withMessage_keepsTheMessage() {
        UploadOutcomeUnknownException exception = new UploadOutcomeUnknownException("reply lost");

        assertEquals("reply lost", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void newException_withCause_keepsMessageAndCause() {
        IOException cause = new IOException("connection reset");

        UploadOutcomeUnknownException exception = new UploadOutcomeUnknownException("reply lost", cause);

        assertEquals("reply lost", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
