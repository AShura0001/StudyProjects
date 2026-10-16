package lavs.business.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionTest {

    @Test
    void newSession_whenCreated_keepsUsernameAndTime() {
        Instant time = Instant.parse("2026-10-17T09:00:00Z");

        Session session = new Session("Group_2", time);

        assertEquals("Group_2", session.username());
        assertEquals(time, session.loggedInAt());
    }

    @Test
    void newSession_withNullUsername_isRejected() {
        assertThrows(NullPointerException.class, () -> new Session(null, Instant.now()));
    }

    @Test
    void newSession_withBlankUsername_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Session("  ", Instant.now()));
    }

    @Test
    void newSession_withNullTime_isRejected() {
        assertThrows(NullPointerException.class, () -> new Session("Group_2", null));
    }
}
