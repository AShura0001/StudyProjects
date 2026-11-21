package lavs.business.service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * A {@link Scheduler} for tests only. Nothing runs by itself and nothing really
 * waits: the test decides when each job runs, on the test's own thread.
 *
 * <p>{@link #runImmediate()} runs the jobs that were asked to run as soon as
 * possible. {@link #fireNextDelayed()} makes the next waiting job run now, as if
 * its delay had passed.
 */
public class ManualScheduler implements Scheduler {

    private static final class DelayedJob implements Handle {
        private final Runnable task;
        private final Duration delay;
        private boolean cancelled;

        private DelayedJob(Runnable task, Duration delay) {
            this.task = task;
            this.delay = delay;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

    private final Deque<Runnable> immediate = new ArrayDeque<>();
    private final List<DelayedJob> delayed = new ArrayList<>();
    private boolean shutDown;

    @Override
    public void execute(Runnable task) {
        immediate.add(Objects.requireNonNull(task));
    }

    @Override
    public Handle schedule(Runnable task, Duration delay) {
        DelayedJob job = new DelayedJob(Objects.requireNonNull(task), Objects.requireNonNull(delay));
        delayed.add(job);
        return job;
    }

    @Override
    public void shutdown() {
        shutDown = true;
    }

    /** Runs every job waiting to run now, including any that those jobs ask for. */
    public void runImmediate() {
        while (!immediate.isEmpty()) {
            immediate.poll().run();
        }
    }

    /**
     * Runs the oldest job that is waiting for its delay and has not been cancelled.
     *
     * @return true if a job ran, false if none was waiting
     */
    public boolean fireNextDelayed() {
        for (DelayedJob job : delayed) {
            if (!job.cancelled) {
                delayed.remove(job);
                job.task.run();
                return true;
            }
        }
        return false;
    }

    /**
     * Gets how many jobs are waiting to run now.
     *
     * @return the number of jobs
     */
    public int immediateCount() {
        return immediate.size();
    }

    /**
     * Gets the delays of the jobs that are waiting and have not been cancelled.
     *
     * @return the delays, oldest job first
     */
    public List<Duration> waitingDelays() {
        return delayed.stream().filter(job -> !job.cancelled).map(job -> job.delay).toList();
    }

    /**
     * Tells whether {@link #shutdown()} has been called.
     *
     * @return true if it has
     */
    public boolean isShutDown() {
        return shutDown;
    }
}
