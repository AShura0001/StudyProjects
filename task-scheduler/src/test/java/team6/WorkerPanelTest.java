package team6;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLongArray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class WorkerPanelTest {

    @Test
    public void describeWorkerShowsIdleWhenCurrentTaskIsUnset() {
        SearchMonitor monitor = SearchMonitor.enabled(2, names(3), weights(3), 2);

        assertEquals("idle", WorkerPanel.describeWorker(monitor, 0));
    }

    @Test
    public void describeWorkerShowsTaskAndProcessorWhenActive() {
        SearchMonitor monitor = SearchMonitor.enabled(2, names(3), weights(3), 2);
        monitor.recordCurrent(1, 2, 1);

        assertEquals("t2 -> P1", WorkerPanel.describeWorker(monitor, 1));
    }

    @Test
    public void describeWorkerReturnsDashForOutOfRangeTaskIndexRatherThanThrowing() throws Exception {
        SearchMonitor monitor = SearchMonitor.enabled(1, names(3), weights(3), 2);
        // Bypasses recordCurrent's assertion to simulate the defensive scenario it guards against:
        // an out-of-range value reaching the panel because assertions are disabled in production.
        Field fieldsField = SearchMonitor.class.getDeclaredField("fields");
        fieldsField.setAccessible(true);
        AtomicLongArray fields = (AtomicLongArray) fieldsField.get(monitor);
        fields.set(2, 99); // worker 0's current-task slot

        assertEquals("-", WorkerPanel.describeWorker(monitor, 0));
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
