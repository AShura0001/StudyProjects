package lavs.business.service;

import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;
import lavs.business.model.DraftType;
import lavs.business.repository.DraftRepository;
import lavs.business.repository.DraftUploadGateway;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.RecordRejectedException;
import lavs.business.repository.UploadOutcomeUnknownException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Keeps staff work safe and sends it to the mainframe when it can.
 *
 * <p>Every record is saved to the local draft store first. Uploading is a
 * separate step that can be repeated, so a dropped connection never costs the
 * staff member their work: a draft that could not be sent simply stays pending
 * and is tried again on the next pass.
 *
 * <p>A draft that belongs to other drafts, such as a loan that belongs to a
 * customer, is only sent once those drafts have been accepted. It is then sent
 * with the real numbers the mainframe gave them.
 *
 * <p>Sending a record twice is the one mistake that cannot be undone, so every
 * attempt is recorded <em>before</em> it starts. If the outcome of an attempt is
 * lost, whether the reply never came back or the application stopped part way
 * through, the draft becomes uncertain. An uncertain draft is never sent again
 * by this service. A person has to check the mainframe and say whether the record
 * is there, using {@link #confirmSynced(String, String)} or
 * {@link #confirmNotSent(String)}.
 *
 * <p>This version does its work on the calling thread, and
 * {@link #syncPending()} must never run at the same time as itself.
 * {@link BackgroundSync} makes sure of that.
 */
public class DraftSyncService {

    /**
     * The field name used for a refusal when the mainframe gave no reason for any
     * particular field, so the message can still be shown.
     */
    public static final String GENERAL_ERROR_FIELD = "general";

    private static final String DEFAULT_REJECTION_MESSAGE = "The mainframe refused this record.";

    /**
     * What one pass over the pending drafts achieved.
     *
     * @param synced how many drafts the mainframe accepted
     * @param rejected how many drafts the mainframe refused
     * @param blocked how many drafts could not be sent because a draft they depend
     *                on was refused, is uncertain, is missing or was never accepted;
     *                they stay pending. Only counted when the pass finished normally.
     * @param uncertain how many drafts became uncertain during this pass, either
     *                  because an attempt lost its outcome or because an earlier
     *                  run stopped in the middle of an attempt
     * @param connectionLost true if the pass stopped early because the mainframe
     *                       could not be reached or an attempt lost its outcome, so
     *                       some drafts were not tried
     */
    public record SyncResult(int synced, int rejected, int blocked, int uncertain, boolean connectionLost) {
    }

    private final DraftRepository repository;
    private final DraftUploadGateway gateway;

    /**
     * Creates a service that stores drafts in the given repository and sends them
     * through the given gateway.
     *
     * @param repository where drafts are saved
     * @param gateway how drafts are sent to the mainframe
     * @throws NullPointerException if either argument is null
     */
    public DraftSyncService(DraftRepository repository, DraftUploadGateway gateway) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.gateway = Objects.requireNonNull(gateway, "gateway must not be null");
    }

    /**
     * Saves a new record that does not depend on any other as a pending draft.
     * Nothing is sent to the mainframe, so this works even when there is no
     * connection.
     *
     * @param type the kind of record
     * @param payload the record as plain text
     * @return the draft that was saved
     * @throws NullPointerException if either argument is null
     * @throws lavs.business.repository.DraftStorageException if the draft could not be saved
     */
    public Draft saveDraft(DraftType type, String payload) {
        return saveDraft(type, payload, List.of());
    }

    /**
     * Saves a new record that depends on other drafts as a pending draft. It will
     * only be sent after those drafts have been accepted by the mainframe.
     *
     * @param type the kind of record
     * @param payload the record as plain text
     * @param dependsOn the local IDs of the drafts the record belongs to, in the
     *                  order the mainframe adapter expects their numbers
     * @return the draft that was saved
     * @throws NullPointerException if any argument, or any ID in the list, is null
     * @throws IllegalArgumentException if an ID is blank or repeated, or no draft
     *                                  with one of the IDs has been saved
     * @throws lavs.business.repository.DraftStorageException if the draft could not be saved
     */
    public Draft saveDraft(DraftType type, String payload, List<String> dependsOn) {
        Draft draft = new Draft(type, payload, dependsOn);
        for (String parentId : draft.getDependsOn()) {
            if (repository.findById(parentId).isEmpty()) {
                throw new IllegalArgumentException("there is no saved draft with ID " + parentId);
            }
        }
        repository.save(draft);
        return draft;
    }

    /**
     * Gets the drafts that may or may not be on the mainframe and are waiting for a
     * person to check.
     *
     * @return the uncertain drafts, oldest first
     * @throws lavs.business.repository.DraftStorageException if the drafts could not be read
     */
    public List<Draft> findUncertain() {
        return repository.findByStatus(DraftStatus.UNCERTAIN);
    }

    /**
     * Records that a person checked the mainframe and found the record of an
     * uncertain draft there. Drafts that were waiting for it can then be sent.
     *
     * @param localId the local ID of the uncertain draft
     * @param serverNumber the number the mainframe gave the record
     * @return the draft, now synced
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if there is no such draft or the number is blank
     * @throws IllegalStateException if the draft is not uncertain
     * @throws lavs.business.repository.DraftStorageException if the draft could not be saved
     */
    public Draft confirmSynced(String localId, String serverNumber) {
        Draft draft = uncertainDraft(localId);
        draft.markSynced(serverNumber);
        repository.save(draft);
        return draft;
    }

    /**
     * Records that a person checked the mainframe and did not find the record of an
     * uncertain draft there. The draft becomes pending and will be sent again.
     *
     * @param localId the local ID of the uncertain draft
     * @return the draft, now pending
     * @throws NullPointerException if the ID is null
     * @throws IllegalArgumentException if there is no such draft
     * @throws IllegalStateException if the draft is not uncertain
     * @throws lavs.business.repository.DraftStorageException if the draft could not be saved
     */
    public Draft confirmNotSent(String localId) {
        Draft draft = uncertainDraft(localId);
        draft.markNotSent();
        repository.save(draft);
        return draft;
    }

    /**
     * Tries to send every pending draft whose dependencies are accepted, and saves
     * what the mainframe answered for each one. Drafts are taken oldest first, but a
     * draft always waits for the drafts it depends on, even if it is older.
     *
     * <p>Before anything else, a draft still marked as sending is turned into an
     * uncertain one. This service never leaves a draft in that state when it
     * finishes normally, so one found at the start of a pass means an earlier run
     * stopped part way through an attempt.
     *
     * <p>Each draft is marked as sending and saved <em>before</em> it is sent.
     *
     * <ul>
     *   <li>Accepted: the draft becomes synced and keeps the number it was given.
     *       Drafts that were waiting for it can now be sent in the same pass.</li>
     *   <li>Refused: the draft becomes rejected and keeps the reasons. Drafts that
     *       depend on it are left pending, and the pass carries on with the rest.</li>
     *   <li>Not reachable, so nothing was sent: the draft goes back to pending and
     *       the pass stops at once, leaving all the later drafts pending.</li>
     *   <li>Outcome unknown: the draft becomes uncertain and the pass stops at once.</li>
     * </ul>
     *
     * @return how many drafts were accepted, refused, left blocked and made
     *         uncertain, and whether the pass stopped early
     * @throws lavs.business.repository.DraftStorageException if a draft could not be read or saved
     */
    public SyncResult syncPending() {
        int uncertain = settleInterruptedAttempts();
        List<Draft> waiting = new ArrayList<>(repository.findPending());
        int synced = 0;
        int rejected = 0;
        boolean progress = true;
        while (progress) {
            progress = false;
            Iterator<Draft> candidates = waiting.iterator();
            while (candidates.hasNext()) {
                Draft draft = candidates.next();
                Optional<List<String>> parentNumbers = parentNumbersOf(draft);
                if (parentNumbers.isEmpty()) {
                    continue;
                }
                candidates.remove();
                progress = true;
                draft.markSending();
                repository.save(draft);
                try {
                    String number = gateway.upload(draft, parentNumbers.get());
                    draft.markSynced(number);
                    repository.save(draft);
                    synced++;
                } catch (RecordRejectedException e) {
                    draft.markRejected(reasonsFor(e));
                    repository.save(draft);
                    rejected++;
                } catch (MainframeUnavailableException e) {
                    draft.markNotSent();
                    repository.save(draft);
                    return new SyncResult(synced, rejected, 0, uncertain, true);
                } catch (UploadOutcomeUnknownException e) {
                    draft.markUncertain();
                    repository.save(draft);
                    return new SyncResult(synced, rejected, 0, uncertain + 1, true);
                }
            }
        }
        return new SyncResult(synced, rejected, waiting.size(), uncertain, false);
    }

    private int settleInterruptedAttempts() {
        int settled = 0;
        for (Draft draft : repository.findByStatus(DraftStatus.SENDING)) {
            draft.markUncertain();
            repository.save(draft);
            settled++;
        }
        return settled;
    }

    private Draft uncertainDraft(String localId) {
        Objects.requireNonNull(localId, "localId must not be null");
        Draft draft = repository.findById(localId)
                .orElseThrow(() -> new IllegalArgumentException("there is no saved draft with ID " + localId));
        if (draft.getStatus() != DraftStatus.UNCERTAIN) {
            throw new IllegalStateException("a " + draft.getStatus() + " draft is not waiting for a check");
        }
        return draft;
    }

    private Optional<List<String>> parentNumbersOf(Draft draft) {
        List<String> numbers = new ArrayList<>();
        for (String parentId : draft.getDependsOn()) {
            Optional<Draft> parent = repository.findById(parentId);
            if (parent.isEmpty() || parent.get().getStatus() != DraftStatus.SYNCED) {
                return Optional.empty();
            }
            numbers.add(parent.get().getServerNumber().orElseThrow());
        }
        return Optional.of(numbers);
    }

    private static Map<String, String> reasonsFor(RecordRejectedException e) {
        if (!e.getFieldErrors().isEmpty()) {
            return e.getFieldErrors();
        }
        String message = e.getMessage() == null || e.getMessage().isBlank()
                ? DEFAULT_REJECTION_MESSAGE
                : e.getMessage();
        return Map.of(GENERAL_ERROR_FIELD, message);
    }
}
