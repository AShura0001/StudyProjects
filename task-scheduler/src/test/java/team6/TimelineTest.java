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
        // Sleeps span the 100ms sampling interval, so each record() call actually lands its
        // own frame instead of being coalesced by offer()'s sampling rule.
        SearchMonitor monitor = SearchMonitor.enabled(1, names(3), weights(3), 2);
        Timeline timeline = new Timeline();

        timeline.record(monitor);
        Thread.sleep(110);
        timeline.record(monitor);
        Thread.sleep(110);
        timeline.record(monitor);

        assertEquals(3, timeline.size());
        long previous = -1;
        for (int i = 0; i < timeline.size(); i++) {
            long elapsed = timeline.frame(i).elapsedMillis();
            assertTrue(elapsed >= previous, "frame " + i + " elapsed " + elapsed + " < previous " + previous);
            previous = elapsed;
        }
    }

    @Test
    public void offerStoresAFrameWheneverTheSnapshotReferenceChangesEvenWithinTheSamplingInterval() {
        // The snapshot-changed condition exists precisely so two improvements landing inside
        // the same 100ms window both get their own frame, instead of the second being dropped.
        SearchMonitor monitor = SearchMonitor.enabled(1, names(3), weights(3), 2);
        Timeline timeline = new Timeline();

        monitor.recordBest(60, new int[] {0, 0, 0}, new int[] {1, 1, 1});
        timeline.record(monitor);

        monitor.recordBest(55, new int[] {0, 0, 0}, new int[] {1, 1, 1}); // a new Snapshot object
        timeline.record(monitor); // called immediately after, well within the 100ms interval

        assertEquals(2, timeline.size());
        assertEquals(60, timeline.frame(0).bestMakespan());
        assertEquals(55, timeline.frame(1).bestMakespan());
    }

    @Test
    public void offerCoalescesRepeatedCallsWithTheSameSnapshotWithinTheSamplingInterval() {
        // The flip side of the rule above: with nothing new to show, rapid repeated calls
        // must not flood the timeline.
        SearchMonitor monitor = SearchMonitor.enabled(1, names(3), weights(3), 2);
        Timeline timeline = new Timeline();

        monitor.recordBest(50, new int[] {0, 0, 0}, new int[] {1, 1, 1});
        timeline.record(monitor);
        timeline.record(monitor);
        timeline.record(monitor);

        assertEquals(1, timeline.size());
    }

    @Test
    public void improvementIndicesReturnsOnlyFramesWhereMakespanDropped() {
        SearchMonitor monitor = SearchMonitor.enabled(1, names(3), weights(3), 2);
        Timeline timeline = new Timeline();

        timeline.record(monitor); // index 0: no schedule yet (-1), not an improvement

        monitor.recordBest(60, new int[] {0, 0, 0}, new int[] {1, 1, 1});
        timeline.record(monitor); // index 1: -1 -> 60, improvement

        monitor.recordBest(55, new int[] {0, 0, 0}, new int[] {1, 1, 1});
        timeline.record(monitor); // index 2: 60 -> 55, improvement

        assertEquals(List.of(1, 2), timeline.improvementIndices());
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

    // ---- frameAtTime ----

    @Test
    public void frameAtTimeReturnsMinusOneOnAnEmptyTimeline() {
        Timeline timeline = new Timeline();

        assertEquals(-1, timeline.frameAtTime(0, 0));
    }

    @Test
    public void frameAtTimeClampsToTheFirstFrameForATimeBeforeIt() {
        Timeline timeline = timelineWithFrameTimes(0, 100, 200, 300);

        assertEquals(0, timeline.frameAtTime(-50, 0));
    }

    @Test
    public void frameAtTimeClampsToTheLastFrameForATimeAfterIt() {
        Timeline timeline = timelineWithFrameTimes(0, 100, 200, 300);

        assertEquals(3, timeline.frameAtTime(10_000, 0));
    }

    @Test
    public void frameAtTimeFindsTheLastFrameAtOrBeforeTheGivenTimeScanningForward() {
        Timeline timeline = timelineWithFrameTimes(0, 100, 200, 300);

        assertEquals(2, timeline.frameAtTime(250, 0));
        assertEquals(1, timeline.frameAtTime(100, 0));
    }

    @Test
    public void frameAtTimeCorrectsBackwardWhenTheHintIndexIsPastTheTarget() {
        Timeline timeline = timelineWithFrameTimes(0, 100, 200, 300);

        // searchFromIndex hints at the last frame, but the target time is much earlier.
        assertEquals(1, timeline.frameAtTime(150, 3));
    }

    // ---- nextImprovement / prevImprovement ----

    @Test
    public void nextAndPrevImprovementReturnMinusOneOnAnEmptyTimeline() {
        Timeline timeline = new Timeline();

        assertEquals(-1, timeline.nextImprovement(-1));
        assertEquals(-1, timeline.prevImprovement(0));
    }

    @Test
    public void nextImprovementFindsTheFirstImprovementAfterFromIndex() {
        Timeline timeline = timelineWithImprovementsAt(5, 1, 3);

        assertEquals(1, timeline.nextImprovement(-1));
        assertEquals(3, timeline.nextImprovement(1));
    }

    @Test
    public void nextImprovementReturnsMinusOneWhenNoneAfterFromIndex() {
        Timeline timeline = timelineWithImprovementsAt(5, 1, 3);

        assertEquals(-1, timeline.nextImprovement(3));
        assertEquals(-1, timeline.nextImprovement(4));
    }

    @Test
    public void prevImprovementFindsTheLastImprovementBeforeFromIndex() {
        Timeline timeline = timelineWithImprovementsAt(5, 1, 3);

        assertEquals(3, timeline.prevImprovement(5));
        assertEquals(1, timeline.prevImprovement(3));
    }

    @Test
    public void prevImprovementReturnsMinusOneWhenNoneBeforeFromIndex() {
        Timeline timeline = timelineWithImprovementsAt(5, 1, 3);

        assertEquals(-1, timeline.prevImprovement(1));
        assertEquals(-1, timeline.prevImprovement(0));
    }

    /** Builds a timeline with one frame per given elapsed time, each with a distinct schedule. */
    private static Timeline timelineWithFrameTimes(long... elapsedTimes) {
        SearchMonitor monitor = SearchMonitor.enabled(1, names(3), weights(3), 2);
        Timeline timeline = new Timeline();
        int makespan = 100;
        for (long elapsed : elapsedTimes) {
            monitor.recordBest(makespan--, new int[] {0, 0, 0}, new int[] {1, 1, 1});
            timeline.offer(new Timeline.Frame(elapsed, 0, 0, 0, monitor.bestMakespan(), monitor.bestSchedule(),
                    new int[1], new int[1], Set.of(), false));
        }
        return timeline;
    }

    /**
     * Builds a timeline of totalFrames frames (elapsedMillis 0, 200, 400, ... 100ms+ apart so
     * every offer lands its own frame) where only the frames at improvementFrameIndices drop
     * the makespan - every other frame re-offers the same, unchanged schedule.
     */
    private static Timeline timelineWithImprovementsAt(int totalFrames, int... improvementFrameIndices) {
        SearchMonitor monitor = SearchMonitor.enabled(1, names(3), weights(3), 2);
        Timeline timeline = new Timeline();
        Set<Integer> improvementIndices = new HashSet<>();
        for (int i : improvementFrameIndices) {
            improvementIndices.add(i);
        }

        int makespan = 100;
        for (int i = 0; i < totalFrames; i++) {
            if (improvementIndices.contains(i)) {
                makespan -= 10;
                monitor.recordBest(makespan, new int[] {0, 0, 0}, new int[] {1, 1, 1});
            }
            timeline.offer(new Timeline.Frame(i * 200L, 0, 0, 0, monitor.bestMakespan(), monitor.bestSchedule(),
                    new int[1], new int[1], Set.of(), false));
        }
        return timeline;
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
