package lavs.business.service;

import lavs.business.model.Draft;
import lavs.business.model.DraftType;
import lavs.business.repository.DraftRepository;
import lavs.business.repository.DraftUploadGateway;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.RecordRejectedException;

import java.util.Map;
import java.util.Objects;

/**
 * Keeps staff work safe and sends it to the mainframe when it can.
 *
 * <p>Every record is saved to the local draft store first. Uploading is a
 * separate step that can be repeated, so a dropped connection never costs the
 * staff member their work: a draft that could not be sent simply stays pending
 * and is tried again on the next pass.
 *
 * <p>This version does its work on the calling thread and sends every draft on
 * its own. Sending a draft after the record it belongs to, and running in the
 * background, come later.
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
     * @param connectionLost true if the pass stopped early because the mainframe
     *                       could not be reached, so some drafts were not tried
     */
    public record SyncResult(int synced, int rejected, boolean connectionLost) {
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
     * Saves a new record as a pending draft. Nothing is sent to the mainframe, so
     * this works even when there is no connection.
     *
     * @param type the kind of record
     * @param payload the record as plain text
     * @return the draft that was saved
     * @throws NullPointerException if either argument is null
     * @throws lavs.business.repository.DraftStorageException if the draft could not be saved
     */
    public Draft saveDraft(DraftType type, String payload) {
        Draft draft = new Draft(type, payload);
        repository.save(draft);
        return draft;
    }

    /**
     * Tries to send every pending draft, oldest first, and saves what the
     * mainframe answered for each one.
     *
     * <ul>
     *   <li>Accepted: the draft becomes synced and keeps the number it was given.</li>
     *   <li>Refused: the draft becomes rejected and keeps the reasons, and the pass
     *       carries on with the next draft.</li>
     *   <li>Not reachable: the pass stops at once and leaves this draft and all the
     *       later ones pending, since nothing is known about whether it arrived.</li>
     * </ul>
     *
     * @return how many drafts were accepted and refused, and whether the pass stopped early
     * @throws lavs.business.repository.DraftStorageException if a draft could not be read or saved
     */
    public SyncResult syncPending() {
        int synced = 0;
        int rejected = 0;
        for (Draft draft : repository.findPending()) {
            try {
                String number = gateway.upload(draft, null);
                draft.markSynced(number);
                repository.save(draft);
                synced++;
            } catch (RecordRejectedException e) {
                draft.markRejected(reasonsFor(e));
                repository.save(draft);
                rejected++;
            } catch (MainframeUnavailableException e) {
                return new SyncResult(synced, rejected, true);
            }
        }
        return new SyncResult(synced, rejected, false);
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
