package team6;

import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Progress channel the branch-and-bound search publishes through, for the
 * visualisation to poll. Plain Java only - no javafx import may appear here, so the
 * search and this class stay usable headlessly and Visualiser remains the only
 * place the scheduler depends on JavaFX.
 *
 * <p>Search threads write with {@code setPlain} - a plain store with no memory
 * fence, the same cost as an ordinary array write - and the UI thread reads with
 * {@code getOpaque}, which stops the JIT from hoisting or caching the load in a
 * register but adds no ordering guarantees beyond that. Neither side pays for
 * cross-thread visibility it does not need: the search never blocks on the UI, and
 * a poll that sees a slightly stale value just repaints it on the next frame.
 */
public final class SearchMonitor {

    // 16 longs = 128 bytes, at least two cache lines, so adjacent workers never share one.
    private static final int FIELDS_PER_WORKER = 16;

    private static final int OFFSET_VISITS = 0;
    private static final int OFFSET_MAX_DEPTH = 1;
    private static final int OFFSET_CURRENT_TASK = 2;
    private static final int OFFSET_CURRENT_PROCESSOR = 3;
    private static final int OFFSET_ACTIVE = 4;

    private static final SearchMonitor DISABLED = new SearchMonitor();

    private final boolean enabled;
    private final int nWorkers;
    private final String[] taskNames;
    private final int[] taskWeights;
    private final int nProcessors;
    private final AtomicLongArray fields;
    private final AtomicReference<Snapshot> best;
    private final long startNanos;

    private SearchMonitor() {
        this.enabled = false;
        this.nWorkers = 0;
        this.taskNames = null;
        this.taskWeights = null;
        this.nProcessors = 0;
        this.fields = null;
        this.best = null;
        this.startNanos = 0;
    }

    private SearchMonitor(int nWorkers, String[] taskNames, int[] taskWeights, int nProcessors) {
        this.enabled = true;
        this.nWorkers = nWorkers;
        this.taskNames = taskNames.clone();
        this.taskWeights = taskWeights.clone();
        this.nProcessors = nProcessors;
        this.fields = new AtomicLongArray(nWorkers * FIELDS_PER_WORKER);
        this.best = new AtomicReference<>();
        this.startNanos = System.nanoTime();
        for (int w = 0; w < nWorkers; w++) {
            fields.setPlain(base(w) + OFFSET_CURRENT_TASK, -1);
            fields.setPlain(base(w) + OFFSET_CURRENT_PROCESSOR, -1);
        }
    }

    /** Returns a monitor whose write side is a no-op and whose read side returns zeros/nulls. */
    public static SearchMonitor disabled() {
        return DISABLED;
    }

    /**
     * Returns a monitor sized for the given search, starting its elapsed-time clock now.
     * taskNames and taskWeights are index-aligned with the taskIndex passed to recordCurrent
     * and Snapshot, so the UI can label and size the Gantt bar for each task.
     */
    public static SearchMonitor enabled(int nWorkers, String[] taskNames, int[] taskWeights, int nProcessors) {
        return new SearchMonitor(nWorkers, taskNames, taskWeights, nProcessors);
    }

    private static int base(int worker) {
        return worker * FIELDS_PER_WORKER;
    }

    // ---- Write side: called from search threads. ----

    /** Records that a worker visited one more search state at the given depth. */
    public void recordVisit(int worker, int depth) {
        if (!enabled) {
            return;
        }
        int b = base(worker);
        fields.setPlain(b + OFFSET_VISITS, fields.getPlain(b + OFFSET_VISITS) + 1);
        if (depth > fields.getPlain(b + OFFSET_MAX_DEPTH)) {
            fields.setPlain(b + OFFSET_MAX_DEPTH, depth);
        }
    }

    /** Records the task/processor pair a worker is currently placing. */
    public void recordCurrent(int worker, int taskIndex, int processorIndex) {
        if (!enabled) {
            return;
        }
        assert taskIndex < taskNames.length;
        assert processorIndex < nProcessors;
        int b = base(worker);
        fields.setPlain(b + OFFSET_CURRENT_TASK, taskIndex);
        fields.setPlain(b + OFFSET_CURRENT_PROCESSOR, processorIndex);
    }

    /**
     * Records a new best schedule. Called rarely - only on improvement - so this may
     * use ordinary synchronisation rather than the lock-free path the per-state
     * counters need.
     */
    public void recordBest(int makespan, int[] startTimes, int[] processors) {
        if (!enabled) {
            return;
        }
        best.set(new Snapshot(makespan, startTimes, processors));
    }

    /** Marks a worker as active. */
    public void workerStarted(int worker) {
        if (!enabled) {
            return;
        }
        fields.setPlain(base(worker) + OFFSET_ACTIVE, 1);
    }

    /** Marks a worker as idle. */
    public void workerFinished(int worker) {
        if (!enabled) {
            return;
        }
        int b = base(worker);
        fields.setPlain(b + OFFSET_ACTIVE, 0);
        fields.setPlain(b + OFFSET_CURRENT_TASK, -1);
        fields.setPlain(b + OFFSET_CURRENT_PROCESSOR, -1);
    }

    // ---- Read side: called from the UI thread. ----

    /** Total search states visited so far, summed across workers. */
    public long statesExplored() {
        if (!enabled) {
            return 0;
        }
        long total = 0;
        for (int w = 0; w < nWorkers; w++) {
            total += fields.getOpaque(base(w) + OFFSET_VISITS);
        }
        return total;
    }

    /** Deepest search depth reached by any worker. */
    public int maxDepth() {
        if (!enabled) {
            return 0;
        }
        long max = 0;
        for (int w = 0; w < nWorkers; w++) {
            max = Math.max(max, fields.getOpaque(base(w) + OFFSET_MAX_DEPTH));
        }
        return (int) max;
    }

    /** Milliseconds elapsed since {@link #enabled} was called. */
    public long elapsedMillis() {
        if (!enabled) {
            return 0;
        }
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /** Length of the best schedule found so far, or -1 if none has been recorded yet. */
    public int bestMakespan() {
        if (!enabled) {
            return -1;
        }
        Snapshot snapshot = best.get();
        return snapshot == null ? -1 : snapshot.makespan();
    }

    /** The best schedule found so far, or null if none has been recorded yet. */
    public Snapshot bestSchedule() {
        if (!enabled) {
            return null;
        }
        return best.get();
    }

    /** Number of workers currently marked active. */
    public int activeWorkers() {
        if (!enabled) {
            return 0;
        }
        int count = 0;
        for (int w = 0; w < nWorkers; w++) {
            if (fields.getOpaque(base(w) + OFFSET_ACTIVE) != 0) {
                count++;
            }
        }
        return count;
    }

    /** The task index a worker is currently placing, or -1 if idle. */
    public int currentTask(int worker) {
        if (!enabled) {
            return -1;
        }
        return (int) fields.getOpaque(base(worker) + OFFSET_CURRENT_TASK);
    }

    /** The processor index a worker is currently placing onto, or -1 if idle. */
    public int currentProcessor(int worker) {
        if (!enabled) {
            return -1;
        }
        return (int) fields.getOpaque(base(worker) + OFFSET_CURRENT_PROCESSOR);
    }

    /** Number of worker threads this monitor was sized for. */
    public int workerCount() {
        return nWorkers;
    }

    /** Number of processors the schedule is being built for. */
    public int processorCount() {
        return nProcessors;
    }

    /** Number of tasks in the graph being scheduled. */
    public int taskCount() {
        return enabled ? taskNames.length : 0;
    }

    /** The name of the task at the given index. */
    public String taskName(int index) {
        return enabled ? taskNames[index] : null;
    }

    /** The weight (duration) of the task at the given index. */
    public int taskWeight(int index) {
        return enabled ? taskWeights[index] : 0;
    }

    /** Immutable snapshot of a schedule: its makespan and per-task start times and processors. */
    public static final class Snapshot {
        private final int makespan;
        private final int[] startTimes;
        private final int[] processors;

        private Snapshot(int makespan, int[] startTimes, int[] processors) {
            this.makespan = makespan;
            this.startTimes = startTimes.clone();
            this.processors = processors.clone();
        }

        public int makespan() {
            return makespan;
        }

        public int[] startTimes() {
            return startTimes.clone();
        }

        public int[] processors() {
            return processors.clone();
        }
    }
}
