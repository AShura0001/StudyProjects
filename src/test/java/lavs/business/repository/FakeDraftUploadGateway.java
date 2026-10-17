package lavs.business.repository;

import lavs.business.model.Draft;
import lavs.business.model.DraftType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A stand-in for the mainframe, for tests only.
 *
 * <p>It can be told to work normally, to be unreachable, or to refuse a particular
 * draft. It keeps a diary of every attempt, in order, so a test can check what
 * was sent, in which order and with which parent number.
 *
 * <p>Numbers are simple and count up for each draft type: {@code CUS-1},
 * {@code CUS-2}, {@code LOAN-1}, {@code COB-1}. Like the real mainframe, it
 * creates a new record every time a draft is accepted, even if the same draft was
 * accepted before, so duplicates show up in the diary.
 */
public class FakeDraftUploadGateway implements DraftUploadGateway {

    /** How one attempt to upload ended. */
    public enum Outcome {
        ACCEPTED,
        UNREACHABLE,
        REJECTED
    }

    /**
     * One entry in the diary.
     *
     * @param draftId the local ID of the draft that was sent
     * @param type the type of the draft that was sent
     * @param parentNumber the parent number that came with the draft, or null
     * @param outcome how the attempt ended
     * @param assignedNumber the number given to the new record, or null if the attempt failed
     */
    public record Attempt(String draftId, DraftType type, String parentNumber,
                          Outcome outcome, String assignedNumber) {
    }

    private final List<Attempt> attempts = new ArrayList<>();
    private final Map<DraftType, Integer> counters = new EnumMap<>(DraftType.class);
    private final Map<String, Map<String, String>> rejections = new HashMap<>();
    private boolean online = true;

    /** Makes every upload fail as if the connection had dropped. */
    public synchronized void goOffline() {
        online = false;
    }

    /** Makes uploads work again after {@link #goOffline()}. */
    public synchronized void goOnline() {
        online = true;
    }

    /**
     * Makes the mainframe refuse one particular draft, every time it is sent.
     *
     * @param draftId the local ID of the draft to refuse
     * @param fieldErrors the reason for each refused field
     */
    public synchronized void rejectDraft(String draftId, Map<String, String> fieldErrors) {
        rejections.put(Objects.requireNonNull(draftId), Map.copyOf(fieldErrors));
    }

    @Override
    public synchronized String upload(Draft draft, String parentNumber) {
        Objects.requireNonNull(draft, "draft must not be null");
        if (!online) {
            record(draft, parentNumber, Outcome.UNREACHABLE, null);
            throw new MainframeUnavailableException("the fake mainframe is offline");
        }
        Map<String, String> errors = rejections.get(draft.getLocalId());
        if (errors != null) {
            record(draft, parentNumber, Outcome.REJECTED, null);
            throw new RecordRejectedException("the fake mainframe refused " + draft.getLocalId(), errors);
        }
        String number = nextNumber(draft.getType());
        record(draft, parentNumber, Outcome.ACCEPTED, number);
        return number;
    }

    /**
     * Gets every attempt so far, oldest first, including the ones that failed.
     *
     * @return a copy of the diary
     */
    public synchronized List<Attempt> attempts() {
        return List.copyOf(attempts);
    }

    /**
     * Gets the local IDs of the drafts that were accepted, in the order they were accepted.
     *
     * @return the accepted draft IDs
     */
    public synchronized List<String> acceptedDraftIds() {
        return attempts.stream()
                .filter(attempt -> attempt.outcome() == Outcome.ACCEPTED)
                .map(Attempt::draftId)
                .toList();
    }

    private void record(Draft draft, String parentNumber, Outcome outcome, String assignedNumber) {
        attempts.add(new Attempt(draft.getLocalId(), draft.getType(), parentNumber, outcome, assignedNumber));
    }

    private String nextNumber(DraftType type) {
        int next = counters.merge(type, 1, Integer::sum);
        return prefix(type) + next;
    }

    private static String prefix(DraftType type) {
        return switch (type) {
            case CUSTOMER -> "CUS-";
            case LOAN -> "LOAN-";
            case COBORROWER -> "COB-";
        };
    }
}
