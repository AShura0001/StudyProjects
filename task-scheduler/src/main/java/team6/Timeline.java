package team6;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Records a replay history of a search, one {@link Frame} at a time, by sampling a
 * {@link SearchMonitor}'s current values. Plain Java only - no javafx import may appear
 * here, so it stays usable and testable headlessly, matching {@link SearchMonitor}.
 *
 * Recording every state the search visits is impossible (tens of millions of them);
 * recording every improvement is free (a handful of them). {@link #record} does not decide
 * when to sample - the caller does, so it can mix a fixed-rate sample with "record whenever
 * the schedule improved" without this class needing to know why it was called.
 */
public final class Timeline {

    private final List<Frame> frames = new ArrayList<>();

    /**
     * Appends a frame built from the monitor's current values. The frame is marked as an
     * improvement if the monitor has a best makespan and it is lower than the previously
     * recorded frame's (or there was no previously recorded makespan at all).
     */
    public void record(SearchMonitor monitor) {
        int workerCount = monitor.workerCount();
        int[] workerTasks = new int[workerCount];
        int[] workerProcessors = new int[workerCount];
        Set<Integer> highlighted = new HashSet<>();
        for (int w = 0; w < workerCount; w++) {
            int taskIndex = monitor.currentTask(w);
            workerTasks[w] = taskIndex;
            workerProcessors[w] = monitor.currentProcessor(w);
            if (taskIndex >= 0) {
                highlighted.add(taskIndex);
            }
        }

        int bestMakespan = monitor.bestMakespan();
        int previousBest = frames.isEmpty() ? -1 : frames.get(frames.size() - 1).bestMakespan();
        boolean improvement = bestMakespan >= 0 && (previousBest == -1 || bestMakespan < previousBest);

        frames.add(new Frame(monitor.elapsedMillis(), monitor.statesExplored(), monitor.maxDepth(),
                monitor.activeWorkers(), bestMakespan, monitor.bestSchedule(),
                workerTasks, workerProcessors, highlighted, improvement));
    }

    /** Number of frames recorded so far. */
    public int size() {
        return frames.size();
    }

    /** The frame at the given index, in recording order. */
    public Frame frame(int index) {
        return frames.get(index);
    }

    /** The most recently recorded frame, or null if nothing has been recorded yet. */
    public Frame last() {
        return frames.isEmpty() ? null : frames.get(frames.size() - 1);
    }

    /** The last frame's elapsed time, or 0 if nothing has been recorded yet. */
    public long durationMillis() {
        Frame last = last();
        return last == null ? 0 : last.elapsedMillis();
    }

    /** Indices, in recording order, of frames where the best makespan dropped. */
    public List<Integer> improvementIndices() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < frames.size(); i++) {
            if (frames.get(i).improvement()) {
                indices.add(i);
            }
        }
        return indices;
    }

    /**
     * One immutable moment of a recorded search: everything the UI needs to redraw the
     * Gantt chart, stats panel and worker panel as they looked at that instant. Arrays and
     * the highlighted set are defensively copied on construction and on every read, so a
     * frame can never be mutated after the fact - frames are replayed later, not live state.
     */
    public record Frame(
            long elapsedMillis,
            long statesExplored,
            int maxDepth,
            int activeWorkers,
            int bestMakespan,
            SearchMonitor.Snapshot schedule,
            int[] workerTasks,
            int[] workerProcessors,
            Set<Integer> highlighted,
            boolean improvement) {

        public Frame {
            workerTasks = workerTasks.clone();
            workerProcessors = workerProcessors.clone();
            highlighted = new HashSet<>(highlighted);
        }

        @Override
        public int[] workerTasks() {
            return workerTasks.clone();
        }

        @Override
        public int[] workerProcessors() {
            return workerProcessors.clone();
        }

        @Override
        public Set<Integer> highlighted() {
            return new HashSet<>(highlighted);
        }
    }
}
