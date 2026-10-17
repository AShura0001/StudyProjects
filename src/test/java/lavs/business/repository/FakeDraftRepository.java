package lavs.business.repository;

import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A draft store that keeps everything in memory, for tests only.
 *
 * <p>It stores and returns <em>copies</em> of drafts, like a real store that
 * writes to disk. A change made to a draft is therefore only remembered if the
 * code under test calls {@link #save(Draft)}, so a forgotten save shows up as a
 * failing test.
 */
public class FakeDraftRepository implements DraftRepository {

    private final Map<String, Draft> stored = new LinkedHashMap<>();
    private boolean failingToSave;

    /**
     * Makes every {@link #save(Draft)} fail as if the disk were full.
     *
     * @param failing true to make saves fail, false to make them work again
     */
    public synchronized void setFailingToSave(boolean failing) {
        this.failingToSave = failing;
    }

    @Override
    public synchronized void save(Draft draft) {
        Objects.requireNonNull(draft, "draft must not be null");
        if (failingToSave) {
            throw new DraftStorageException("the fake disk is full");
        }
        stored.put(draft.getLocalId(), copyOf(draft));
    }

    @Override
    public synchronized Optional<Draft> findById(String localId) {
        return Optional.ofNullable(stored.get(localId)).map(FakeDraftRepository::copyOf);
    }

    @Override
    public synchronized List<Draft> findPending() {
        return stored.values().stream()
                .filter(draft -> draft.getStatus() == DraftStatus.PENDING)
                .sorted(Comparator.comparing(Draft::getCreatedAt).thenComparing(Draft::getLocalId))
                .map(FakeDraftRepository::copyOf)
                .toList();
    }

    @Override
    public synchronized void delete(String localId) {
        stored.remove(localId);
    }

    private static Draft copyOf(Draft draft) {
        return new Draft(draft.getLocalId(), draft.getType(), draft.getPayload(), draft.getStatus(),
                draft.getCreatedAt(), draft.getServerNumber().orElse(null), draft.getErrors());
    }
}
