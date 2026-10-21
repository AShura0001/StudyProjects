package lavs.infrastructure.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Stopping waits for work already running, so nothing touches the drafts folder afterwards. */
class ExecutorSchedulerStopTest {

    @Test
    void awaitStopped_returnsOnlyAfterTheRunningTaskHasFinished() throws InterruptedException {
        ExecutorScheduler scheduler = new ExecutorScheduler();
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean finished = new AtomicBoolean(false);
        scheduler.execute(() -> {
            started.countDown();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            finished.set(true);
        });
        assertTrue(started.await(2, TimeUnit.SECONDS));

        scheduler.shutdown();
        assertTrue(scheduler.awaitStopped(Duration.ofSeconds(5)));
        assertTrue(finished.get(), "the task had finished by the time awaitStopped returned");
    }

    @Test
    void awaitStopped_doesNotWaitForARetryThatHasNotStarted() {
        ExecutorScheduler scheduler = new ExecutorScheduler();
        scheduler.schedule(() -> { }, Duration.ofMinutes(5));

        scheduler.shutdown();
        long before = System.nanoTime();
        assertTrue(scheduler.awaitStopped(Duration.ofSeconds(5)));
        assertTrue(System.nanoTime() - before < Duration.ofSeconds(2).toNanos(), "delayed retries are dropped");
    }
}
