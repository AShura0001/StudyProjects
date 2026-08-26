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
 * <p>Recording every state the search visits is impossible (tens of millions of them);
 * recording every improvement is free (a handful of them). Capture and storage are split
 * in two: {@link #capture} builds a frame from the monitor with no side effects, so the
 * caller can render it immediately; {@link #offer} then decides whether that frame is worth
 * keeping for replay, so this class - not the caller - owns the sampling policy.
 */
public final class Timeline {

    private static final long INITIAL_RECORD_INTERVAL_MILLIS = 100;
    private static final int RECORD_INTERVAL_DOUBLING_THRESHOLD = 20_000;

    private final List<Frame> frames = new ArrayList<>();
    private SearchMonitor.Snapshot lastStoredSnapshot = null;
    private long lastStoredElapsedMillis;
    private boolean hasStoredFrame = false;
    private long recordIntervalMillis = INITIAL_RECORD_INTERVAL_MILLIS;

    /** Builds a frame from the monitor's current values. Does not store it - see {@link #offer}. */
    public static Frame capture(SearchMonitor monitor) {
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

        return new Frame(monitor.elapsedMillis(), monitor.statesExplored(), monitor.maxDepth(),
                monitor.activeWorkers(), monitor.bestMakespan(), monitor.bestSchedule(),
                workerTasks, workerProcessors, highlighted, false);
    }

    /**
     * Stores the frame if it passes the sampling rule: at least the current interval (100ms,
     * doubled once storage exceeds 20,000 frames, as a memory guard on long runs) has passed
     * since the last stored frame's elapsed time, or its schedule reference differs from the
     * last stored frame's - so an improvement always lands its own frame even if two improvements
     * happen inside the same interval, which a fixed-rate sample alone would silently merge.
     */
    public void offer(Frame frame) {
        boolean snapshotChanged = frame.schedule() != lastStoredSnapshot;
        boolean intervalElapsed = !hasStoredFrame
                || frame.elapsedMillis() - lastStoredElapsedMillis >= recordIntervalMillis;
        if (!snapshotChanged && !intervalElapsed) {
            return;
        }

        int bestMakespan = frame.bestMakespan();
        int previousBest = frames.isEmpty() ? -1 : frames.get(frames.size() - 1).bestMakespan();
        boolean improvement = bestMakespan >= 0 && (previousBest == -1 || bestMakespan < previousBest);

        frames.add(new Frame(frame.elapsedMillis(), frame.statesExplored(), frame.maxDepth(),
                frame.activeWorkers(), bestMakespan, frame.schedule(),
                frame.workerTasks(), frame.workerProcessors(), frame.highlighted(), improvement));

        lastStoredSnapshot = frame.schedule();
        lastStoredElapsedMillis = frame.elapsedMillis();
        hasStoredFrame = true;
        if (frames.size() > RECORD_INTERVAL_DOUBLING_THRESHOLD) {
            recordIntervalMillis *= 2;
        }
    }

    /** Convenience wrapper: {@code offer(capture(monitor))}. */
    public void record(SearchMonitor monitor) {
        offer(capture(monitor));
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
     * The index of the last frame whose elapsedMillis is less than or equal to the given
     * time. Scans forward from searchFromIndex (clamped into range) first, since replay
     * typically moves forward and this keeps a repeated scrub close to O(1); corrects
     * backward afterwards if the target time turns out to be earlier than that. Clamps to 0
     * for a time before the first frame and size-1 for a time after the last. Returns -1 on
     * an empty timeline.
     */
    public int frameAtTime(long elapsedMillis, int searchFromIndex) {
        if (frames.isEmpty()) {
            return -1;
        }
        int index = Math.max(0, Math.min(searchFromIndex, frames.size() - 1));
        while (index + 1 < frames.size() && frames.get(index + 1).elapsedMillis() <= elapsedMillis) {
            index++;
        }
        while (index > 0 && frames.get(index).elapsedMillis() > elapsedMillis) {
            index--;
        }
        return index;
    }

    /** Index of the first improvement frame after fromIndex, or -1 if there is none. */
    public int nextImprovement(int fromIndex) {
        for (int i = fromIndex + 1; i < frames.size(); i++) {
            if (frames.get(i).improvement()) {
                return i;
            }
        }
        return -1;
    }

    /** Index of the last improvement frame before fromIndex, or -1 if there is none. */
    public int prevImprovement(int fromIndex) {
        for (int i = Math.min(fromIndex, frames.size()) - 1; i >= 0; i--) {
            if (frames.get(i).improvement()) {
                return i;
            }
        }
        return -1;
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
