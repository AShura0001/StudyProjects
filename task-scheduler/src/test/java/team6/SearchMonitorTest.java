package team6;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class SearchMonitorTest {

    @Test
    public void countersAccumulatePerWorkerAndSumAcrossWorkers() {
        SearchMonitor monitor = SearchMonitor.enabled(3, names(10), weights(10), 2);

        monitor.recordVisit(0, 1);
        monitor.recordVisit(0, 2);
        monitor.recordVisit(1, 1);
        monitor.recordVisit(2, 1);
        monitor.recordVisit(2, 1);
        monitor.recordVisit(2, 1);

        assertEquals(6, monitor.statesExplored());
    }

    @Test
    public void concurrentWorkersDoNotCorruptEachOthersSlots() throws InterruptedException {
        int nWorkers = 8;
        int visitsPerWorker = 200_000;
        SearchMonitor monitor = SearchMonitor.enabled(nWorkers, names(10), weights(10), 2);

        CountDownLatch ready = new CountDownLatch(nWorkers);
        CountDownLatch go = new CountDownLatch(1);
        Thread[] threads = new Thread[nWorkers];
        for (int w = 0; w < nWorkers; w++) {
            int worker = w;
            threads[w] = new Thread(() -> {
                ready.countDown();
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < visitsPerWorker; i++) {
                    monitor.recordVisit(worker, i % 50);
                }
            });
            threads[w].start();
        }
        ready.await();
        go.countDown();
        for (Thread t : threads) {
            t.join();
        }

        assertEquals((long) nWorkers * visitsPerWorker, monitor.statesExplored());
        assertEquals(49, monitor.maxDepth());
    }

    @Test
    public void maxDepthReportsMaximumSeenNotLatest() {
        SearchMonitor monitor = SearchMonitor.enabled(1, names(10), weights(10), 2);

        monitor.recordVisit(0, 5);
        monitor.recordVisit(0, 9);
        monitor.recordVisit(0, 3);

        assertEquals(9, monitor.maxDepth());
    }

    @Test
    public void bestScheduleReturnsACopyIndependentOfLaterMutation() {
        SearchMonitor monitor = SearchMonitor.enabled(1, names(10), weights(10), 2);
        int[] startTimes = {0, 1, 2};
        int[] processors = {0, 1, 0};

        monitor.recordBest(19, startTimes, processors);
        startTimes[0] = 99;
        processors[0] = 99;

        SearchMonitor.Snapshot snapshot = monitor.bestSchedule();
        assertArrayEquals(new int[] {0, 1, 2}, snapshot.startTimes());
        assertArrayEquals(new int[] {0, 1, 0}, snapshot.processors());
        assertEquals(19, snapshot.makespan());
        assertEquals(19, monitor.bestMakespan());
    }

    @Test
    public void bestMakespanIsMinusOneUntilRecorded() {
        SearchMonitor monitor = SearchMonitor.enabled(1, names(10), weights(10), 2);

        assertEquals(-1, monitor.bestMakespan());
        assertNull(monitor.bestSchedule());
    }

    @Test
    public void disabledRecordsNothingAndNeverThrowsOnRead() {
        SearchMonitor monitor = SearchMonitor.disabled();

        monitor.recordVisit(0, 5);
        monitor.recordCurrent(0, 1, 1);
        monitor.recordBest(19, new int[] {1}, new int[] {1});
        monitor.workerStarted(0);
        monitor.workerFinished(0);

        assertEquals(0, monitor.statesExplored());
        assertEquals(0, monitor.maxDepth());
        assertEquals(0, monitor.elapsedMillis());
        assertEquals(-1, monitor.bestMakespan());
        assertNull(monitor.bestSchedule());
        assertEquals(0, monitor.activeWorkers());
        assertEquals(-1, monitor.currentTask(0));
        assertEquals(-1, monitor.currentProcessor(0));
        assertEquals(0, monitor.workerCount());
        assertEquals(0, monitor.processorCount());
        assertEquals(0, monitor.taskCount());
        assertNull(monitor.taskName(0));
        assertEquals(0, monitor.taskWeight(0));
    }

    @Test
    public void activeWorkersTracksStartedAndFinished() {
        SearchMonitor monitor = SearchMonitor.enabled(3, names(10), weights(10), 2);

        monitor.workerStarted(0);
        monitor.workerStarted(1);
        assertEquals(2, monitor.activeWorkers());

        monitor.workerFinished(0);
        assertEquals(1, monitor.activeWorkers());
        assertEquals(-1, monitor.currentTask(0));
        assertEquals(-1, monitor.currentProcessor(0));
    }

    @Test
    public void currentTaskAndProcessorAreMinusOneUntilRecorded() {
        SearchMonitor monitor = SearchMonitor.enabled(2, names(10), weights(10), 2);

        assertEquals(-1, monitor.currentTask(1));
        assertEquals(-1, monitor.currentProcessor(1));

        monitor.recordCurrent(1, 4, 1);

        assertEquals(4, monitor.currentTask(1));
        assertEquals(1, monitor.currentProcessor(1));
    }

    @Test
    public void elapsedMillisIsNonNegativeAndNonDecreasing() throws InterruptedException {
        SearchMonitor monitor = SearchMonitor.enabled(1, names(10), weights(10), 2);

        long first = monitor.elapsedMillis();
        assertTrue(first >= 0);
        Thread.sleep(10);
        long second = monitor.elapsedMillis();

        assertFalse(second < first);
    }

    @Test
    public void countsAndTaskMetadataMatchWhatWasPassedToEnabled() {
        String[] names = {"a", "b", "c"};
        int[] weights = {5, 3, 7};
        SearchMonitor monitor = SearchMonitor.enabled(4, names, weights, 2);

        assertEquals(4, monitor.workerCount());
        assertEquals(2, monitor.processorCount());
        assertEquals(3, monitor.taskCount());
        assertEquals("a", monitor.taskName(0));
        assertEquals("b", monitor.taskName(1));
        assertEquals("c", monitor.taskName(2));
        assertEquals(5, monitor.taskWeight(0));
        assertEquals(3, monitor.taskWeight(1));
        assertEquals(7, monitor.taskWeight(2));
    }

    @Test
    public void taskMetadataIsDefensivelyCopiedFromTheArraysPassedToEnabled() {
        String[] names = {"a", "b"};
        int[] weights = {5, 3};
        SearchMonitor monitor = SearchMonitor.enabled(1, names, weights, 2);

        names[0] = "mutated";
        weights[0] = 99;

        assertEquals("a", monitor.taskName(0));
        assertEquals(5, monitor.taskWeight(0));
    }

    @Test
    public void elapsedMillisFreezesAfterMarkFinished() throws InterruptedException {
        SearchMonitor monitor = SearchMonitor.enabled(1, names(10), weights(10), 2);

        Thread.sleep(10);
        monitor.markFinished();
        long frozen = monitor.elapsedMillis();
        Thread.sleep(10);

        assertEquals(frozen, monitor.elapsedMillis());
        assertTrue(monitor.isFinished());
    }

    @Test
    public void markFinishedTwiceDoesNotChangeTheFrozenValue() throws InterruptedException {
        SearchMonitor monitor = SearchMonitor.enabled(1, names(10), weights(10), 2);

        Thread.sleep(10);
        monitor.markFinished();
        long frozen = monitor.elapsedMillis();
        Thread.sleep(10);
        monitor.markFinished();

        assertEquals(frozen, monitor.elapsedMillis());
    }

    @Test
    public void markStartedResetsTheElapsedClock() throws InterruptedException {
        SearchMonitor monitor = SearchMonitor.enabled(1, names(10), weights(10), 2);

        Thread.sleep(30);
        monitor.markStarted();
        long afterReset = monitor.elapsedMillis();

        assertTrue(afterReset < 30);
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
