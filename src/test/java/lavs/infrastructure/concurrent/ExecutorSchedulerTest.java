package lavs.infrastructure.concurrent;

import lavs.business.service.Scheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutorSchedulerTest {

    private static final long WAIT_SECONDS = 5;

    private final ExecutorScheduler scheduler = new ExecutorScheduler();

    @AfterEach
    void stopTheWorker() {
        scheduler.shutdown();
    }

    @Test
    void execute_runsTheJobOnItsOwnNamedDaemonThread() throws InterruptedException {
        AtomicReference<Thread> ranOn = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        scheduler.execute(() -> {
            ranOn.set(Thread.currentThread());
            done.countDown();
        });

        assertTrue(done.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertNotSame(Thread.currentThread(), ranOn.get());
        assertEquals("draft-sync-worker", ranOn.get().getName());
        assertTrue(ranOn.get().isDaemon());
    }

    @Test
    void execute_withSeveralJobs_runsThemOneAtATimeInOrder() throws InterruptedException {
        List<Integer> order = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(3);

        for (int i = 1; i <= 3; i++) {
            int number = i;
            scheduler.execute(() -> {
                order.add(number);
                done.countDown();
            });
        }

        assertTrue(done.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertEquals(List.of(1, 2, 3), order);
    }

    @Test
    void schedule_runsTheJobOnlyAfterTheDelay() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);

        scheduler.schedule(done::countDown, Duration.ofMillis(300));

        assertFalse(done.await(50, TimeUnit.MILLISECONDS));
        assertTrue(done.await(WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    void cancel_beforeTheDelayEnds_stopsTheJobFromRunning() throws InterruptedException {
        AtomicBoolean ran = new AtomicBoolean(false);
        Scheduler.Handle handle = scheduler.schedule(() -> ran.set(true), Duration.ofMillis(150));

        handle.cancel();
        Thread.sleep(400);

        assertFalse(ran.get());
    }

    @Test
    void shutdown_dropsJobsThatAreStillWaiting() throws InterruptedException {
        AtomicBoolean ran = new AtomicBoolean(false);
        scheduler.schedule(() -> ran.set(true), Duration.ofMillis(150));

        scheduler.shutdown();
        Thread.sleep(400);

        assertFalse(ran.get());
    }

    @Test
    void execute_afterShutdown_isRefused() {
        scheduler.shutdown();

        assertThrows(RejectedExecutionException.class, () -> scheduler.execute(() -> { }));
    }

    @Test
    void execute_withNullJob_isRejected() {
        assertThrows(NullPointerException.class, () -> scheduler.execute(null));
    }

    @Test
    void schedule_withNullJobOrDelay_isRejected() {
        assertThrows(NullPointerException.class, () -> scheduler.schedule(null, Duration.ofSeconds(1)));
        assertThrows(NullPointerException.class, () -> scheduler.schedule(() -> { }, null));
    }
}
