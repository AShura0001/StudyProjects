package team6;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class TimelineTest {

    @Test
    public void elapsedMillisIsNonDecreasingAcrossFrames() throws InterruptedException {
        SearchMonitor monitor = SearchMonitor.enabled(1, names(3), weights(3), 2);
        Timeline timeline = new Timeline();

        timeline.record(monitor);
        Thread.sleep(10);
        timeline.record(monitor);
        Thread.sleep(10);
        timeline.record(monitor);

        long previous = -1;
        for (int i = 0; i < timeline.size(); i++) {
            long elapsed = timeline.frame(i).elapsedMillis();
            assertTrue(elapsed >= previous, "frame " + i + " elapsed " + elapsed + " < previous " + previous);
            previous = elapsed;
        }
    }

    @Test
    public void recordAppendsAFrameOnEveryCallRegardlessOfTiming() {
        // Timeline itself has no notion of a sampling interval - that decision belongs to the
        // caller (Visualiser's 100ms/snapshot-change rule). Calling record() twice back-to-back,
        // both within whatever window the caller might use, must still append two frames.
        SearchMonitor monitor = SearchMonitor.enabled(1, names(3), weights(3), 2);
        Timeline timeline = new Timeline();

        monitor.recordBest(50, new int[] {0, 0, 0}, new int[] {1, 1, 1});
        timeline.record(monitor);
        timeline.record(monitor);

        assertEquals(2, timeline.size());
        assertEquals(50, timeline.frame(0).bestMakespan());
        assertEquals(50, timeline.frame(1).bestMakespan());
        assertSame(monitor.bestSchedule(), timeline.frame(0).schedule());
        assertSame(monitor.bestSchedule(), timeline.frame(1).schedule());
    }

    @Test
    public void improvementIndicesReturnsOnlyFramesWhereMakespanDropped() {
        SearchMonitor monitor = SearchMonitor.enabled(1, names(3), weights(3), 2);
        Timeline timeline = new Timeline();

        timeline.record(monitor); // index 0: no schedule yet (-1), not an improvement

        monitor.recordBest(60, new int[] {0, 0, 0}, new int[] {1, 1, 1});
        timeline.record(monitor); // index 1: -1 -> 60, improvement
        timeline.record(monitor); // index 2: 60 -> 60, unchanged, not an improvement

        monitor.recordBest(55, new int[] {0, 0, 0}, new int[] {1, 1, 1});
        timeline.record(monitor); // index 3: 60 -> 55, improvement
        timeline.record(monitor); // index 4: 55 -> 55, unchanged, not an improvement

        assertEquals(List.of(1, 3), timeline.improvementIndices());
    }

    @Test
    public void lastReflectsTheMostRecentlyRecordedState() {
        SearchMonitor monitor = SearchMonitor.enabled(1, names(3), weights(3), 2);
        Timeline timeline = new Timeline();

        assertNull(timeline.last());

        timeline.record(monitor);
        monitor.recordBest(40, new int[] {0, 0, 0}, new int[] {1, 1, 1});
        timeline.record(monitor);

        assertSame(timeline.frame(timeline.size() - 1), timeline.last());
        assertEquals(40, timeline.last().bestMakespan());
    }

    @Test
    public void frameIsImmutableAgainstConstructorArgumentMutationAndReturnedCopies() {
        int[] tasks = {1, 2};
        int[] processors = {1, 2};
        Set<Integer> highlighted = new HashSet<>(Set.of(1, 2));

        Timeline.Frame frame = new Timeline.Frame(100, 5, 3, 2, 40, null, tasks, processors, highlighted, true);

        // Mutating what was passed to the constructor must not affect the stored frame.
        tasks[0] = 999;
        processors[0] = 999;
        highlighted.add(999);

        assertEquals(1, frame.workerTasks()[0]);
        assertEquals(1, frame.workerProcessors()[0]);
        assertFalse(frame.highlighted().contains(999));

        // Mutating what the accessors return must not affect the stored frame either.
        frame.workerTasks()[0] = 999;
        frame.workerProcessors()[0] = 999;
        frame.highlighted().add(999);

        assertEquals(1, frame.workerTasks()[0]);
        assertEquals(1, frame.workerProcessors()[0]);
        assertFalse(frame.highlighted().contains(999));
    }

    @Test
    public void durationMillisIsZeroOnAnEmptyTimelineAndDoesNotThrow() {
        Timeline timeline = new Timeline();

        assertEquals(0, timeline.durationMillis());
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
