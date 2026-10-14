package lavs.business.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One saved record (a customer, a loan or a co-borrower) that is waiting to be
 * confirmed by the mainframe.
 *
 * <p>The record itself is kept as plain text in the payload, so this class does
 * not depend on any JSON library. The local ID, type, payload and creation time
 * never change; only the status does.
 */
public class Draft {

    private final String localId;
    private final DraftType type;
    private final String payload;
    private final Instant createdAt;
    private DraftStatus status;

    /**
     * Creates a brand new draft with a fresh unique ID, a status of
     * {@link DraftStatus#PENDING} and the current time as its creation time.
     *
     * @param type the kind of record this draft holds
     * @param payload the record as plain text
     * @throws NullPointerException if any argument is null
     */
    public Draft(DraftType type, String payload) {
        this(UUID.randomUUID().toString(), type, payload, DraftStatus.PENDING, Instant.now());
    }

    /**
     * Rebuilds a draft that already exists, for example when it is loaded from
     * disk. The ID, status and creation time are kept exactly as they were.
     *
     * @param localId the unique ID the draft was created with
     * @param type the kind of record this draft holds
     * @param payload the record as plain text
     * @param status where the draft is in its life
     * @param createdAt the moment the draft was first created
     * @throws NullPointerException if any argument is null
     */
    public Draft(String localId, DraftType type, String payload, DraftStatus status, Instant createdAt) {
        this.localId = Objects.requireNonNull(localId, "localId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * Gets the unique ID this draft was created with.
     *
     * @return the local ID
     */
    public String getLocalId() {
        return localId;
    }

    /**
     * Gets the kind of record this draft holds.
     *
     * @return the draft type
     */
    public DraftType getType() {
        return type;
    }

    /**
     * Gets the record as plain text.
     *
     * @return the payload
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Gets the moment this draft was first created.
     *
     * @return the creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets where this draft is in its life.
     *
     * @return the current status
     */
    public DraftStatus getStatus() {
        return status;
    }

    /**
     * Changes where this draft is in its life.
     *
     * @param status the new status
     * @throws NullPointerException if the status is null
     */
    public void setStatus(DraftStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }
}
