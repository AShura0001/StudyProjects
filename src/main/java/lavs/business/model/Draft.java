package lavs.business.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One saved record (a customer, a loan or a co-borrower) that is waiting to be
 * confirmed by the mainframe.
 *
 * <p>The record itself is kept as plain text in the payload, so this class does
 * not depend on any JSON library. The local ID, type, creation time and the list
 * of drafts this one depends on never change.
 *
 * <p>A draft can only be in one of these states, and the methods that change its
 * status keep it that way:
 *
 * <ul>
 *   <li>{@link DraftStatus#PENDING}, {@link DraftStatus#SENDING} and
 *       {@link DraftStatus#UNCERTAIN}: no server number and no errors.</li>
 *   <li>{@link DraftStatus#SYNCED}: a server number and no errors. It never changes again.</li>
 *   <li>{@link DraftStatus#REJECTED}: at least one error and no server number.</li>
 * </ul>
 *
 * <p>A draft is always marked as sending, and saved, <em>before</em> an attempt to
 * send it. That is what lets a crash or a lost reply be recognised afterwards
 * instead of silently sending the record twice. The steps are:
 *
 * <pre>
 * PENDING   --markSending--&gt;   SENDING
 * SENDING   --markSynced--&gt;    SYNCED
 * SENDING   --markRejected--&gt;  REJECTED
 * SENDING   --markNotSent--&gt;   PENDING     (it certainly never left)
 * SENDING   --markUncertain--&gt; UNCERTAIN   (it may have arrived)
 * UNCERTAIN --markSynced--&gt;    SYNCED      (a person found it on the mainframe)
 * UNCERTAIN --markNotSent--&gt;   PENDING     (a person confirmed it is not there)
 * REJECTED / PENDING --revise--&gt; PENDING
 * </pre>
 */
public class Draft {

    private final String localId;
    private final DraftType type;
    private final Instant createdAt;
    private final List<String> dependsOn;
    private String payload;
    private DraftStatus status;
    private String serverNumber;
    private Map<String, String> errors;
    /**
     * Steps of a send that are known to have reached the mainframe, such as the number
     * a new customer was given. A retry skips them, so it never repeats a step.
     */
    private final Map<String, String> checkpoints;

    /**
     * Creates a brand new draft that does not depend on any other draft, with a
     * fresh unique ID, a status of {@link DraftStatus#PENDING} and the current
     * time as its creation time.
     *
     * @param type the kind of record this draft holds
     * @param payload the record as plain text
     * @throws NullPointerException if any argument is null
     */
    public Draft(DraftType type, String payload) {
        this(type, payload, List.of());
    }

    /**
     * Creates a brand new draft that can only be sent once the drafts it depends
     * on have been accepted by the mainframe.
     *
     * @param type the kind of record this draft holds
     * @param payload the record as plain text
     * @param dependsOn the local IDs of the drafts this one belongs to, in the order
     *                  the mainframe adapter expects their numbers
     * @throws NullPointerException if any argument, or any ID in the list, is null
     * @throws IllegalArgumentException if an ID is blank or appears twice
     */
    public Draft(DraftType type, String payload, List<String> dependsOn) {
        this(UUID.randomUUID().toString(), type, payload, DraftStatus.PENDING, Instant.now(),
                null, Map.of(), dependsOn);
    }

    /**
     * Rebuilds a draft that already exists and does not depend on any other draft.
     * See the longer constructor for the rules.
     *
     * @param localId the unique ID the draft was created with
     * @param type the kind of record this draft holds
     * @param payload the record as plain text
     * @param status where the draft is in its life
     * @param createdAt the moment the draft was first created
     * @param serverNumber the number the mainframe assigned, or null if it has none
     * @param errors the reason for each refused field, empty if there are none
     * @throws NullPointerException if the local ID, type, payload, status, creation
     *                              time or errors are null
     * @throws IllegalArgumentException if the status does not match the server
     *                                  number and errors
     */
    public Draft(String localId, DraftType type, String payload, DraftStatus status, Instant createdAt,
                 String serverNumber, Map<String, String> errors) {
        this(localId, type, payload, status, createdAt, serverNumber, errors, List.of());
    }

    /**
     * Rebuilds a draft that already exists, for example when it is loaded from
     * disk. Everything is kept exactly as it was, but the combination of status,
     * server number and errors must be one the draft can really be in.
     *
     * @param localId the unique ID the draft was created with
     * @param type the kind of record this draft holds
     * @param payload the record as plain text
     * @param status where the draft is in its life
     * @param createdAt the moment the draft was first created
     * @param serverNumber the number the mainframe assigned, or null if it has none
     * @param errors the reason for each refused field, empty if there are none
     * @param dependsOn the local IDs of the drafts this one belongs to, empty if none
     * @throws NullPointerException if the local ID, type, payload, status, creation
     *                              time, errors or dependencies are null, or any
     *                              dependency is null
     * @throws IllegalArgumentException if the status does not match the server
     *                                  number and errors, or a dependency is blank,
     *                                  repeated or the draft's own ID
     */
    public Draft(String localId, DraftType type, String payload, DraftStatus status, Instant createdAt,
                 String serverNumber, Map<String, String> errors, List<String> dependsOn) {
        this(localId, type, payload, status, createdAt, serverNumber, errors, dependsOn, Map.of());
    }

    /**
     * Rebuilds a saved draft, including the steps of an earlier send that already reached the mainframe.
     */
    public Draft(String localId, DraftType type, String payload, DraftStatus status, Instant createdAt,
                 String serverNumber, Map<String, String> errors, List<String> dependsOn,
                 Map<String, String> checkpoints) {
        this.checkpoints = new LinkedHashMap<>(Objects.requireNonNull(checkpoints, "checkpoints must not be null"));
        this.localId = Objects.requireNonNull(localId, "localId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.serverNumber = serverNumber;
        this.errors = copyOf(Objects.requireNonNull(errors, "errors must not be null"));
        this.dependsOn = checkedDependencies(Objects.requireNonNull(dependsOn, "dependsOn must not be null"));
        requireConsistent();
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
     * Gets the local IDs of the drafts this one cannot be sent without, in the
     * order they were given.
     *
     * @return an unmodifiable list, empty if the draft depends on nothing
     */
    public List<String> getDependsOn() {
        return dependsOn;
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
     * Gets the number the mainframe assigned to this record.
     *
     * @return the server number, or empty if the mainframe has not accepted it
     */
    public Optional<String> getServerNumber() {
        return Optional.ofNullable(serverNumber);
    }

    /**
     * Gets the reason the mainframe refused each field, in the order given.
     *
     * @return an unmodifiable map from field name to reason, empty unless rejected
     */
    public Map<String, String> getErrors() {
        return errors;
    }

    /** @return the steps already done, by name; empty for a draft never partly sent */
    public Map<String, String> getCheckpoints() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(checkpoints));
    }

    /** @return the value recorded for a finished step, or empty if it has not been done */
    public Optional<String> checkpoint(String step) {
        return Optional.ofNullable(checkpoints.get(step));
    }

    /**
     * Records that one step of the send reached the mainframe. Only allowed while the
     * draft is waiting or being sent.
     *
     * @throws IllegalStateException if the draft is uncertain, refused or already sent
     */
    public void recordCheckpoint(String step, String value) {
        requireStatus("given a checkpoint", DraftStatus.PENDING, DraftStatus.SENDING);
        requireNotBlank(step, "step");
        Objects.requireNonNull(value, "value must not be null");
        checkpoints.put(step, value);
    }

    /**
     * Records that an attempt to send this draft is about to start. The draft must
     * be saved in this state before anything is sent.
     *
     * @throws IllegalStateException if the draft is not pending
     */
    public void markSending() {
        requireStatus("marked as sending", DraftStatus.PENDING);
        this.status = DraftStatus.SENDING;
    }

    /**
     * Records that the mainframe accepted this draft, or that a person found it
     * there after an uncertain attempt. Any errors are cleared.
     *
     * @param number the number the mainframe assigned to the record
     * @throws NullPointerException if the number is null
     * @throws IllegalArgumentException if the number is blank
     * @throws IllegalStateException if the draft is not sending or uncertain
     */
    public void markSynced(String number) {
        requireStatus("marked as synced", DraftStatus.SENDING, DraftStatus.UNCERTAIN);
        requireNotBlank(number, "number");
        this.status = DraftStatus.SYNCED;
        this.serverNumber = number;
        this.errors = Map.of();
    }

    /**
     * Records that the mainframe refused this draft.
     *
     * @param fieldErrors the reason for each refused field, at least one
     * @throws NullPointerException if the errors are null
     * @throws IllegalArgumentException if there are no errors
     * @throws IllegalStateException if the draft is not sending
     */
    public void markRejected(Map<String, String> fieldErrors) {
        requireStatus("marked as rejected", DraftStatus.SENDING);
        Objects.requireNonNull(fieldErrors, "fieldErrors must not be null");
        if (fieldErrors.isEmpty()) {
            throw new IllegalArgumentException("a rejected draft needs at least one error");
        }
        this.status = DraftStatus.REJECTED;
        this.serverNumber = null;
        this.errors = copyOf(fieldErrors);
    }

    /**
     * Records that an attempt to send this draft ended without anyone knowing
     * whether it reached the mainframe.
     *
     * @throws IllegalStateException if the draft is not sending
     */
    public void markUncertain() {
        requireStatus("marked as uncertain", DraftStatus.SENDING);
        this.status = DraftStatus.UNCERTAIN;
    }

    /**
     * Puts the draft back to pending because it is known not to be on the mainframe:
     * either the attempt never left, or a person checked and did not find it.
     *
     * @throws IllegalStateException if the draft is not sending or uncertain
     */
    public void markNotSent() {
        requireStatus("marked as not sent", DraftStatus.SENDING, DraftStatus.UNCERTAIN);
        this.status = DraftStatus.PENDING;
    }

    /**
     * Replaces the record with a new version, for example when the staff member
     * edits it or corrects what the mainframe refused. The draft becomes pending
     * again and any errors are cleared.
     *
     * @param newPayload the new version of the record as plain text
     * @throws NullPointerException if the payload is null
     * @throws IllegalStateException if the draft is sending, uncertain or synced
     */
    public void revise(String newPayload) {
        Objects.requireNonNull(newPayload, "newPayload must not be null");
        requireStatus("revised", DraftStatus.PENDING, DraftStatus.REJECTED);
        this.payload = newPayload;
        this.status = DraftStatus.PENDING;
        this.errors = Map.of();
    }

    private void requireStatus(String action, DraftStatus... allowed) {
        for (DraftStatus option : allowed) {
            if (status == option) {
                return;
            }
        }
        throw new IllegalStateException("a " + status + " draft cannot be " + action);
    }

    private void requireConsistent() {
        boolean hasNumber = serverNumber != null;
        boolean hasErrors = !errors.isEmpty();
        boolean consistent = switch (status) {
            case PENDING, SENDING, UNCERTAIN -> !hasNumber && !hasErrors;
            case SYNCED -> hasNumber && !serverNumber.isBlank() && !hasErrors;
            case REJECTED -> !hasNumber && hasErrors;
        };
        if (!consistent) {
            throw new IllegalArgumentException("a " + status + " draft cannot have server number "
                    + serverNumber + " and " + errors.size() + " errors");
        }
    }

    private List<String> checkedDependencies(List<String> parents) {
        List<String> copy = new ArrayList<>(parents.size());
        for (String parent : parents) {
            requireNotBlank(parent, "a dependency");
            if (parent.equals(localId)) {
                throw new IllegalArgumentException("a draft cannot depend on itself");
            }
            copy.add(parent);
        }
        if (new HashSet<>(copy).size() != copy.size()) {
            throw new IllegalArgumentException("a draft cannot depend on the same draft twice");
        }
        return List.copyOf(copy);
    }

    private static void requireNotBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static Map<String, String> copyOf(Map<String, String> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
