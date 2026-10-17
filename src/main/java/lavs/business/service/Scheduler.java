package lavs.business.service;

import java.time.Duration;

/**
 * Runs jobs one at a time on a single worker, now or after a delay.
 *
 * <p>Everything that talks to the mainframe or changes drafts is given to one
 * worker, so two jobs can never overlap. That keeps the mainframe connection to
 * one thread at a time and means two passes can never send the same draft twice.
 *
 * <p>The business layer only knows about this interface, so tests can use a
 * version that runs jobs only when the test says so, with no real waiting.
 */
public interface Scheduler {

    /** A job that was scheduled for later and can still be cancelled. */
    interface Handle {

        /** Stops the job from running if it has not started yet. Does nothing otherwise. */
        void cancel();
    }

    /**
     * Runs a job on the worker as soon as the jobs before it have finished.
     *
     * @param task the job to run
     */
    void execute(Runnable task);

    /**
     * Runs a job on the worker after a delay.
     *
     * @param task the job to run
     * @param delay how long to wait first
     * @return a handle that can cancel the job while it is still waiting
     */
    Handle schedule(Runnable task, Duration delay);

    /** Stops the worker. Jobs that are still waiting for their delay are dropped. */
    void shutdown();
}
