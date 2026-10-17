package lavs.business.service;

import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;
import lavs.business.model.DraftType;
import lavs.business.repository.FakeDraftRepository;
import lavs.business.repository.FakeDraftUploadGateway;
import lavs.business.service.DraftSyncService.SyncResult;
import lavs.infrastructure.concurrent.ExecutorScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The same sync, but on a real worker thread with very short real delays, to
 * prove the pieces work together outside the controlled test scheduler.
 */
class BackgroundSyncRealThreadTest {

    private static final long WAIT_SECONDS = 5;

    private final FakeDraftRepository repository = new FakeDraftRepository();
    private final FakeDraftUploadGateway gateway = new FakeDraftUploadGateway();
    private final CountDownLatch connectionLost = new CountDownLatch(1);
    private final CountDownLatch synced = new CountDownLatch(1);
    private final AtomicReference<Thread> listenerThread = new AtomicReference<>();
    private final BackgroundSync sync = new BackgroundSync(
            new DraftSyncService(repository, gateway),
            new ExecutorScheduler(),
            new RetryPolicy(Duration.ofMillis(10), Duration.ofMillis(40), () -> 1.0),
            new SyncListener() {
                @Override
                public void onConnectionLost(SyncResult result, Duration retryIn) {
                    listenerThread.compareAndSet(null, Thread.currentThread());
                    connectionLost.countDown();
                }

                @Override
                public void onSyncFinished(SyncResult result) {
                    if (result.synced() > 0) {
                        synced.countDown();
                    }
                }
            });

    @AfterEach
    void stopTheWorker() {
        sync.stop();
    }

    @Test
    void saveDraft_whenTheConnectionDropsAndReturns_sendsTheDraftByItself() throws InterruptedException {
        gateway.goOffline();

        Draft draft = sync.saveDraft(DraftType.CUSTOMER, "customer text");

        assertTrue(connectionLost.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertEquals(DraftStatus.PENDING, repository.findById(draft.getLocalId()).orElseThrow().getStatus());
        gateway.goOnline();
        assertTrue(synced.await(WAIT_SECONDS, TimeUnit.SECONDS));
        Draft stored = repository.findById(draft.getLocalId()).orElseThrow();
        assertEquals(DraftStatus.SYNCED, stored.getStatus());
        assertEquals("CUS-1", stored.getServerNumber().orElseThrow());
    }

    @Test
    void listener_isCalledOnTheWorkerThreadNotTheCallersThread() throws InterruptedException {
        gateway.goOffline();

        sync.saveDraft(DraftType.CUSTOMER, "customer text");

        assertTrue(connectionLost.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertNotNull(listenerThread.get());
        assertNotSame(Thread.currentThread(), listenerThread.get());
        assertEquals("draft-sync-worker", listenerThread.get().getName());
    }

    @Test
    void saveDraft_whileTheMainframeIsSlow_returnsWithoutWaiting() {
        gateway.goOffline();

        long started = System.nanoTime();
        sync.saveDraft(DraftType.CUSTOMER, "customer text");
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertTrue(elapsedMillis < 1000, "saving took " + elapsedMillis + " ms");
    }
}
