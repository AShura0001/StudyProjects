package lavs.business.model;

import java.util.Objects;
import java.util.UUID;

// One saved record (a customer, a loan or a co-borrower) that is waiting to be
// confirmed by the mainframe. The payload is the record itself, kept as plain text.
public class Draft {

    private final String localId;
    private final DraftType type;
    private final String payload;
    private DraftStatus status;

    // Used when a brand new draft is created: gets a fresh ID and starts as PENDING.
    public Draft(DraftType type, String payload) {
        this(UUID.randomUUID().toString(), type, payload, DraftStatus.PENDING);
    }

    // Used when an existing draft is rebuilt, for example when it is loaded from disk.
    public Draft(String localId, DraftType type, String payload, DraftStatus status) {
        this.localId = Objects.requireNonNull(localId, "localId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public String getLocalId() {
        return localId;
    }

    public DraftType getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public DraftStatus getStatus() {
        return status;
    }

    public void setStatus(DraftStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }
}
