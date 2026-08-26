package team6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class GanttChartTest {

    @Test
    public void niceTickStepReturnsValuesFromTheOneTwoFiveSequence() {
        for (int makespan : new int[] { 1, 3, 7, 19, 50, 100, 999, 12345 }) {
            for (int targetTicks : new int[] { 1, 4, 8, 15 }) {
                double step = GanttChart.niceTickStep(makespan, targetTicks);
                double magnitude = Math.pow(10, Math.floor(Math.log10(step)));
                double normalized = step / magnitude;
                assertTrue(
                        approxEquals(normalized, 1) || approxEquals(normalized, 2) || approxEquals(normalized, 5),
                        "step " + step + " for makespan " + makespan + " normalizes to " + normalized);
            }
        }
    }

    @Test
    public void niceTickStepNeverReturnsZero() {
        assertTrue(GanttChart.niceTickStep(0, 8) > 0);
        assertTrue(GanttChart.niceTickStep(1, 8) > 0);
        assertTrue(GanttChart.niceTickStep(0, 1) > 0);
    }

    @Test
    public void niceTickStepOnMakespan100TargetingEightTicksGivesASensibleCount() {
        double step = GanttChart.niceTickStep(100, 8);
        double tickCount = 100 / step;
        assertTrue(tickCount >= 4 && tickCount <= 16, "tick count was " + tickCount);
    }

    @Test
    public void barXScalesLinearly() {
        assertEquals(48.0, GanttChart.barX(0, 2.0, 48.0));
        assertEquals(58.0, GanttChart.barX(5, 2.0, 48.0));
        assertEquals(68.0, GanttChart.barX(10, 2.0, 48.0));
    }

    @Test
    public void barWidthScalesLinearly() {
        assertEquals(20.0, GanttChart.barWidth(10, 2.0));
        assertEquals(40.0, GanttChart.barWidth(20, 2.0));
    }

    @Test
    public void barWidthNeverReturnsLessThanTwo() {
        assertEquals(2.0, GanttChart.barWidth(0, 2.0));
        assertEquals(2.0, GanttChart.barWidth(1, 0.01));
    }

    @Test
    public void rowIndexConvertsOneBasedProcessorNumberToZeroBasedRow() {
        assertEquals(0, GanttChart.rowIndex(1));
        for (int p = 1; p <= 8; p++) {
            assertEquals(p - 1, GanttChart.rowIndex(p));
        }
    }

    @Test
    public void snapshotWithTaskOnHighestProcessorNumberIsNotSkipped() {
        int processorCount = 4;
        SearchMonitor monitor = SearchMonitor.enabled(1, names(3), weights(3), processorCount);
        monitor.recordBest(10, new int[] { 0, 0, 0 }, new int[] { 1, 2, processorCount });

        SearchMonitor.Snapshot snapshot = monitor.bestSchedule();
        int[] processors = snapshot.processors();
        int lastTaskRow = GanttChart.rowIndex(processors[2]);

        assertEquals(processorCount - 1, lastTaskRow);
        assertTrue(lastTaskRow >= 0 && lastTaskRow < processorCount);
    }

    private static boolean approxEquals(double a, double b) {
        return Math.abs(a - b) < 1e-9;
    }

    private static String[] names(int count) {
        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = "T" + i;
        }
        return names;
    }

    private static int[] weights(int count) {
        int[] weights = new int[count];
        for (int i = 0; i < count; i++) {
            weights[i] = i + 1;
        }
        return weights;
    }
}
