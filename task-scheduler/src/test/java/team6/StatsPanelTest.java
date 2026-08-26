package team6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class StatsPanelTest {

    @Test
    public void formatElapsedFormatsMinutesSecondsAndTenths() {
        assertEquals("0:00.0", StatsPanel.formatElapsed(0));
        assertEquals("0:04.2", StatsPanel.formatElapsed(4234));
        assertEquals("1:05.0", StatsPanel.formatElapsed(65000));
    }

    @Test
    public void formatCountUsesThousandsSeparatorsOrMillionsAbbreviation() {
        assertEquals("999", StatsPanel.formatCount(999));
        assertEquals("1,234", StatsPanel.formatCount(1234));
        assertEquals("1.4M", StatsPanel.formatCount(1_400_000));
    }
}
