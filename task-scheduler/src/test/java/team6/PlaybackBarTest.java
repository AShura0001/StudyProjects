package team6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class PlaybackBarTest {

    @Test
    public void tickXAtFirstIndexIsAtTheThumbInset() {
        assertEquals(7.0, PlaybackBar.tickX(0, 10, 300, 7), 1e-9);
    }

    @Test
    public void tickXAtLastIndexIsAtTheFarEndOfTheThumbTravel() {
        assertEquals(293.0, PlaybackBar.tickX(9, 10, 300, 7), 1e-9);
    }

    @Test
    public void tickXWithASingleFrameDoesNotDivideByZero() {
        assertEquals(7.0, PlaybackBar.tickX(0, 1, 300, 7), 1e-9);
    }
}
