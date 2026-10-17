package lavs.business.repository;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class MainframeUnavailableExceptionTest {

    @Test
    void newException_withCause_keepsMessageAndCause() {
        IOException cause = new IOException("connection reset");

        MainframeUnavailableException exception = new MainframeUnavailableException("dropped", cause);

        assertEquals("dropped", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
