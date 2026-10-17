package lavs.infrastructure.concurrent;

import lavs.business.service.Scheduler;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The real {@link Scheduler}: one background thread that runs jobs one at a time.
 *
 * <p>The thread is a daemon, so it never stops the application from closing.
 * After {@link #shutdown()}, jobs that are still waiting for their delay are
 * dropped, and asking to run a new job throws
 * {@link java.util.concurrent.RejectedExecutionException}.
 */
public class ExecutorScheduler implements Scheduler {

    private static final String THREAD_NAME = "draft-sync-worker";

    private final ScheduledThreadPoolExecutor executor;

    /** Creates a scheduler and starts its worker thread when the first job arrives. */
    public ExecutorScheduler() {
        executor = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(Objects.requireNonNull(task, "task must not be null"));
    }

    @Override
    public Handle schedule(Runnable task, Duration delay) {
        Objects.requireNonNull(task, "task must not be null");
        Objects.requireNonNull(delay, "delay must not be null");
        ScheduledFuture<?> future = executor.schedule(task, delay.toNanos(), TimeUnit.NANOSECONDS);
        return () -> future.cancel(false);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }
}
