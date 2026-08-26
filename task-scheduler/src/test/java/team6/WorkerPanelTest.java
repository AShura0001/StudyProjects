package team6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class WorkerPanelTest {

    @Test
    public void describeWorkerShowsIdleWhenTaskIndexIsNegative() {
        SearchMonitor monitor = SearchMonitor.enabled(2, names(3), weights(3), 2);

        assertEquals("idle", WorkerPanel.describeWorker(monitor, -1, -1));
    }

    @Test
    public void describeWorkerShowsTaskAndProcessorWhenActive() {
        SearchMonitor monitor = SearchMonitor.enabled(2, names(3), weights(3), 2);

        assertEquals("t2 -> P1", WorkerPanel.describeWorker(monitor, 2, 1));
    }

    @Test
    public void describeWorkerReturnsDashForOutOfRangeTaskIndexRatherThanThrowing() {
        // Simulates the defensive scenario the guard exists for: an out-of-range value
        // reaching the panel because SearchMonitor's own assertion is disabled in production.
        SearchMonitor monitor = SearchMonitor.enabled(1, names(3), weights(3), 2);

        assertEquals("-", WorkerPanel.describeWorker(monitor, 99, 1));
    }

    private static String[] names(int n) {
        String[] names = new String[n];
        for (int i = 0; i < n; i++) {
            names[i] = "t" + i;
        }
        return names;
    }

    private static int[] weights(int n) {
        int[] weights = new int[n];
        for (int i = 0; i < n; i++) {
            weights[i] = i + 1;
        }
        return weights;
    }
}
