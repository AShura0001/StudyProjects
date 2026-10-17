package lavs.business.repository;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordRejectedExceptionTest {

    @Test
    void newException_withMessageAndErrors_keepsBoth() {
        Map<String, String> errors = Map.of("principal", "Must be greater than zero.");

        RecordRejectedException exception = new RecordRejectedException("refused", errors);

        assertEquals("refused", exception.getMessage());
        assertEquals(errors, exception.getFieldErrors());
    }

    @Test
    void getFieldErrors_withManyErrors_keepsTheOrderTheyWereGiven() {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("startDate", "Bad date.");
        errors.put("principal", "Too small.");
        errors.put("rate", "Too high.");

        RecordRejectedException exception = new RecordRejectedException("refused", errors);

        assertEquals(List.of("startDate", "principal", "rate"),
                List.copyOf(exception.getFieldErrors().keySet()));
    }

    @Test
    void getFieldErrors_whenTheOriginalMapChangesLater_isNotAffected() {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("principal", "Too small.");
        RecordRejectedException exception = new RecordRejectedException("refused", errors);

        errors.put("rate", "Too high.");

        assertEquals(Map.of("principal", "Too small."), exception.getFieldErrors());
    }

    @Test
    void getFieldErrors_whenCallerTriesToChangeIt_isRejected() {
        RecordRejectedException exception = new RecordRejectedException("refused", Map.of());

        assertThrows(UnsupportedOperationException.class,
                () -> exception.getFieldErrors().put("rate", "Too high."));
    }

    @Test
    void newException_withNoFieldReasons_hasAnEmptyMap() {
        RecordRejectedException exception = new RecordRejectedException("refused", Map.of());

        assertTrue(exception.getFieldErrors().isEmpty());
    }

    @Test
    void newException_withNullErrors_isRejected() {
        assertThrows(NullPointerException.class,
                () -> new RecordRejectedException("refused", null));
    }
}
