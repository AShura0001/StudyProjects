package lavs.business.repository;

import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;
import lavs.business.model.DraftType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeDraftRepositoryTest {

    private final FakeDraftRepository repository = new FakeDraftRepository();

    @Test
    void findById_afterSave_returnsTheDraft() {
        Draft draft = new Draft(DraftType.LOAN, "text");
        repository.save(draft);

        assertEquals("text", repository.findById(draft.getLocalId()).orElseThrow().getPayload());
    }

    @Test
    void findById_withUnknownId_returnsEmpty() {
        assertTrue(repository.findById("nope").isEmpty());
    }

    @Test
    void save_thenChangingTheDraftWithoutSavingAgain_doesNotChangeWhatIsStored() {
        Draft draft = new Draft(DraftType.LOAN, "text");
        repository.save(draft);

        draft.markSynced("LOAN-1");

        assertEquals(DraftStatus.PENDING, repository.findById(draft.getLocalId()).orElseThrow().getStatus());
    }

    @Test
    void findPending_withMixedDrafts_returnsOnlyPendingOldestFirst() {
        Instant early = Instant.parse("2026-10-01T09:00:00Z");
        Instant late = Instant.parse("2026-10-02T09:00:00Z");
        repository.save(new Draft("late", DraftType.LOAN, "x", DraftStatus.PENDING, late, null, Map.of()));
        repository.save(new Draft("done", DraftType.LOAN, "x", DraftStatus.SYNCED, early, "LOAN-1", Map.of()));
        repository.save(new Draft("early", DraftType.LOAN, "x", DraftStatus.PENDING, early, null, Map.of()));

        List<String> ids = repository.findPending().stream().map(Draft::getLocalId).toList();

        assertEquals(List.of("early", "late"), ids);
    }

    @Test
    void delete_withExistingDraft_removesIt() {
        Draft draft = new Draft(DraftType.LOAN, "text");
        repository.save(draft);

        repository.delete(draft.getLocalId());

        assertTrue(repository.findById(draft.getLocalId()).isEmpty());
    }

    @Test
    void save_whenSetToFail_throwsStorageExceptionAndStoresNothing() {
        Draft draft = new Draft(DraftType.LOAN, "text");
        repository.setFailingToSave(true);

        assertThrows(DraftStorageException.class, () -> repository.save(draft));

        assertTrue(repository.findById(draft.getLocalId()).isEmpty());
    }
}
