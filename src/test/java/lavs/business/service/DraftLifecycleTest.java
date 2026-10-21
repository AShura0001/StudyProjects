package lavs.business.service;

import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;
import lavs.business.model.DraftType;
import lavs.business.repository.DraftUploadGateway;
import lavs.business.repository.FakeDraftRepository;
import lavs.business.repository.MainframeUnavailableException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Fixing, discarding and clearing out drafts, and saving the progress of a multi-step send. */
class DraftLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-10-22T09:00:00Z");

    private final FakeDraftRepository repository = new FakeDraftRepository();
    private final DraftSyncService service = new DraftSyncService(repository, (draft, parents) -> "N-1",
            Clock.fixed(NOW, ZoneOffset.UTC));

    private Draft stored(String id, DraftStatus status, Instant created, List<String> dependsOn) {
        Draft draft = new Draft(id, DraftType.LOAN, "payload " + id, status, created,
                status == DraftStatus.SYNCED ? "LN-" + id : null,
                status == DraftStatus.REJECTED ? Map.of("general", "refused") : Map.of(), dependsOn);
        repository.save(draft);
        return draft;
    }

    @Test
    void revise_ofARefusedDraft_queuesItAgain_andKeepsItsProgress() {
        repository.save(new Draft("r1", DraftType.CUSTOMER, "old", DraftStatus.REJECTED, NOW,
                null, Map.of("general", "refused"), List.of(), Map.of("customer", "CUS-1")));

        Draft revised = service.revise("r1", "new");

        assertEquals(DraftStatus.PENDING, revised.getStatus());
        assertEquals("new", repository.findById("r1").orElseThrow().getPayload());
        assertEquals("CUS-1", repository.findById("r1").orElseThrow().checkpoint("customer").orElseThrow());
        assertTrue(revised.getErrors().isEmpty());
    }

    @Test
    void revise_ofASentDraft_isRefused() {
        stored("s1", DraftStatus.SYNCED, NOW, List.of());
        assertThrows(IllegalStateException.class, () -> service.revise("s1", "x"));
    }

    @Test
    void discard_deletesAnUnsentDraft() {
        stored("p1", DraftStatus.PENDING, NOW, List.of());
        service.discard("p1");
        assertTrue(repository.findById("p1").isEmpty());
    }

    @Test
    void discard_isRefused_whenAnotherUnsentDraftNeedsIt_orItWasSent() {
        stored("parent", DraftStatus.REJECTED, NOW, List.of());
        stored("child", DraftStatus.PENDING, NOW, List.of("parent"));
        stored("sent", DraftStatus.SYNCED, NOW, List.of());

        assertThrows(IllegalStateException.class, () -> service.discard("parent"));
        assertThrows(IllegalStateException.class, () -> service.discard("sent"));
        assertTrue(repository.findById("parent").isPresent());
    }

    @Test
    void removeOldSent_keepsRecentOnes_andOnesStillNeeded() {
        Instant old = NOW.minus(DraftSyncService.KEEP_SENT_FOR).minus(Duration.ofHours(1));
        stored("old", DraftStatus.SYNCED, old, List.of());
        stored("recent", DraftStatus.SYNCED, NOW.minus(Duration.ofDays(1)), List.of());
        stored("oldParent", DraftStatus.SYNCED, old, List.of());
        stored("waitingChild", DraftStatus.PENDING, NOW, List.of("oldParent"));
        repository.findById("waitingChild").orElseThrow();

        assertEquals(1, service.removeOldSent());

        assertTrue(repository.findById("old").isEmpty());
        assertTrue(repository.findById("recent").isPresent());
        assertTrue(repository.findById("oldParent").isPresent(), "its number is still needed");
    }

    @Test
    void progressRecordedDuringASend_isSaved_evenWhenTheConnectionThenDrops() {
        DraftUploadGateway halfway = new DraftUploadGateway() {
            @Override
            public String upload(Draft draft, List<String> parentNumbers) {
                throw new AssertionError("the progress-saving form should be used");
            }

            @Override
            public String upload(Draft draft, List<String> parentNumbers, Runnable saveProgress) {
                draft.recordCheckpoint("customer", "CUS-9");
                saveProgress.run();
                throw new MainframeUnavailableException("gone");
            }
        };
        DraftSyncService sync = new DraftSyncService(repository, halfway, Clock.fixed(NOW, ZoneOffset.UTC));
        Draft draft = sync.saveDraft(DraftType.CUSTOMER, "customer");

        assertTrue(sync.syncPending().connectionLost());

        Draft after = repository.findById(draft.getLocalId()).orElseThrow();
        assertEquals(DraftStatus.PENDING, after.getStatus());
        assertEquals("CUS-9", after.checkpoint("customer").orElseThrow());
    }

    @Test
    void backgroundSync_reviseAndDiscard_askForAPass() {
        ManualScheduler scheduler = new ManualScheduler();
        BackgroundSync background = new BackgroundSync(service, scheduler, RetryPolicy.standard());
        stored("r2", DraftStatus.REJECTED, NOW, List.of());
        stored("p2", DraftStatus.PENDING, NOW, List.of());

        background.discard("p2");
        assertTrue(repository.findById("p2").isEmpty());
        scheduler.runImmediate();

        background.revise("r2", "fixed");
        assertEquals(1, scheduler.immediateCount(), "the fixed draft is sent on the next pass");
        scheduler.runImmediate();
        assertEquals(DraftStatus.SYNCED, repository.findById("r2").orElseThrow().getStatus());
    }

    @Test
    void afterEveryPass_theListenerHearsHowManyOldDraftsStillNeedAPerson() {
        stored("refusedLastWeek", DraftStatus.REJECTED, NOW.minus(Duration.ofDays(3)), List.of());
        stored("unsure", DraftStatus.UNCERTAIN, NOW, List.of());
        int[] heard = {-1};
        ManualScheduler scheduler = new ManualScheduler();
        BackgroundSync background = new BackgroundSync(service, scheduler, RetryPolicy.standard(), new SyncListener() {
            @Override
            public void onNeedsChecking(int count) {
                heard[0] = count;
            }
        });

        background.requestSync();
        scheduler.runImmediate();

        assertEquals(2, heard[0]);
    }
}
