package lavs.business.repository;

import lavs.business.model.Draft;

import java.util.List;
import java.util.Optional;

/**
 * The list of jobs a draft store must be able to do.
 *
 * <p>The business layer only knows about this interface, so the real storage
 * (JSON files) can be swapped for a fake one in tests.
 *
 * <p>Every method throws {@link DraftStorageException} if the store cannot do
 * its job, for example because the disk is full or a file is damaged.
 */
public interface DraftRepository {

    /**
     * Saves a draft. If a draft with the same local ID is already stored, it is
     * replaced. This is also how a status change is stored: change the status on
     * the draft, then save it again.
     *
     * @param draft the draft to store, must not be null
     * @throws DraftStorageException if the draft could not be written
     */
    void save(Draft draft);

    /**
     * Finds one draft by its local ID.
     *
     * @param localId the local ID of the draft to find
     * @return the draft, or an empty result if there is no draft with that ID
     * @throws DraftStorageException if the stored drafts could not be read
     */
    Optional<Draft> findById(String localId);

    /**
     * Finds every draft whose status is {@code PENDING}, oldest first, so the
     * same order is used after every restart.
     *
     * @return the pending drafts, or an empty list if there are none
     * @throws DraftStorageException if the stored drafts could not be read
     */
    List<Draft> findPending();

    /**
     * Removes a draft for good. Does nothing if there is no draft with that ID.
     *
     * @param localId the local ID of the draft to remove
     * @throws DraftStorageException if the draft could not be removed
     */
    void delete(String localId);
}
