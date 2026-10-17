package lavs.business.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualSchedulerTest {

    private final ManualScheduler scheduler = new ManualScheduler();
    private final List<String> log = new ArrayList<>();

    @Test
    void execute_doesNotRunTheJobUntilTheTestSaysSo() {
        scheduler.execute(() -> log.add("ran"));

        assertTrue(log.isEmpty());
        assertEquals(1, scheduler.immediateCount());
    }

    @Test
    void runImmediate_runsJobsInOrderIncludingOnesAddedWhileRunning() {
        scheduler.execute(() -> {
            log.add("first");
            scheduler.execute(() -> log.add("third"));
        });
        scheduler.execute(() -> log.add("second"));

        scheduler.runImmediate();

        assertEquals(List.of("first", "second", "third"), log);
    }

    @Test
    void schedule_remembersTheDelayAndDoesNotRunTheJob() {
        scheduler.schedule(() -> log.add("later"), Duration.ofSeconds(4));

        assertEquals(List.of(Duration.ofSeconds(4)), scheduler.waitingDelays());
        assertTrue(log.isEmpty());
    }

    @Test
    void fireNextDelayed_runsTheOldestWaitingJobOnce() {
        scheduler.schedule(() -> log.add("one"), Duration.ofSeconds(1));
        scheduler.schedule(() -> log.add("two"), Duration.ofSeconds(2));

        assertTrue(scheduler.fireNextDelayed());

        assertEquals(List.of("one"), log);
        assertEquals(List.of(Duration.ofSeconds(2)), scheduler.waitingDelays());
    }

    @Test
    void fireNextDelayed_whenNothingIsWaiting_returnsFalse() {
        assertFalse(scheduler.fireNextDelayed());
    }

    @Test
    void cancel_onAWaitingJob_stopsItFromRunning() {
        Scheduler.Handle handle = scheduler.schedule(() -> log.add("later"), Duration.ofSeconds(1));

        handle.cancel();

        assertFalse(scheduler.fireNextDelayed());
        assertTrue(scheduler.waitingDelays().isEmpty());
        assertTrue(log.isEmpty());
    }

    @Test
    void shutdown_isRemembered() {
        assertFalse(scheduler.isShutDown());

        scheduler.shutdown();

        assertTrue(scheduler.isShutDown());
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
