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
 * <p>It can be told to work normally, to be unreachable, to refuse a particular
 * draft, or to lose the reply or the request of the next uploads. It keeps a diary
 * of every attempt, in order, so a test can check what was sent, in which order
 * and with which parent numbers, and how many records the mainframe really holds.
 *
 * <p>Numbers are simple and count up for each draft type: {@code CUS-1},
 * {@code CUS-2}, {@code LOAN-1}, {@code COB-1}. Like the real mainframe, it
 * creates a new record every time a draft is accepted, even if the same draft was
 * accepted before, so duplicates show up in the diary.
 */
public class FakeDraftUploadGateway implements DraftUploadGateway {

    /** How one attempt to upload ended. */
    public enum Outcome {
        /** The mainframe saved the record and the reply arrived. */
        ACCEPTED,
        /** The mainframe could not be reached, and nothing was sent. */
        UNREACHABLE,
        /** The mainframe refused the record. */
        REJECTED,
        /** The mainframe saved the record, but the reply was lost. */
        REPLY_LOST,
        /** The request was lost on the way, so the mainframe never saw it. */
        REQUEST_LOST
    }

    /**
     * One entry in the diary.
     *
     * @param draftId the local ID of the draft that was sent
     * @param type the type of the draft that was sent
     * @param parentNumbers the parent numbers that came with the draft
     * @param outcome how the attempt ended
     * @param assignedNumber the number the mainframe gave the new record, or null
     *                       if the mainframe created nothing
     */
    public record Attempt(String draftId, DraftType type, List<String> parentNumbers,
                          Outcome outcome, String assignedNumber) {
    }

    private final List<Attempt> attempts = new ArrayList<>();
    private final Map<DraftType, Integer> counters = new EnumMap<>(DraftType.class);
    private final Map<String, Map<String, String>> rejections = new HashMap<>();
    private boolean online = true;
    private int repliesToLose;
    private int requestsToLose;

    /** Makes every upload fail as if the connection had dropped before anything was sent. */
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

    /**
     * Stops refusing a draft, for example because the staff member corrected it.
     *
     * @param draftId the local ID of the draft to accept from now on
     */
    public synchronized void stopRejecting(String draftId) {
        rejections.remove(draftId);
    }

    /**
     * Makes the next few uploads reach the mainframe and be saved there, but lose
     * the reply, so the caller gets an {@link UploadOutcomeUnknownException}.
     *
     * @param count how many uploads to affect
     */
    public synchronized void loseNextReplies(int count) {
        repliesToLose = count;
    }

    /**
     * Makes the next few uploads get lost on the way, so the mainframe never sees
     * them, but the caller still gets an {@link UploadOutcomeUnknownException}
     * because it cannot tell.
     *
     * @param count how many uploads to affect
     */
    public synchronized void loseNextRequests(int count) {
        requestsToLose = count;
    }

    @Override
    public synchronized String upload(Draft draft, List<String> parentNumbers) {
        Objects.requireNonNull(draft, "draft must not be null");
        Objects.requireNonNull(parentNumbers, "parentNumbers must not be null");
        if (!online) {
            record(draft, parentNumbers, Outcome.UNREACHABLE, null);
            throw new MainframeUnavailableException("the fake mainframe is offline");
        }
        if (requestsToLose > 0) {
            requestsToLose--;
            record(draft, parentNumbers, Outcome.REQUEST_LOST, null);
            throw new UploadOutcomeUnknownException("the fake request was lost on the way");
        }
        Map<String, String> errors = rejections.get(draft.getLocalId());
        if (errors != null) {
            record(draft, parentNumbers, Outcome.REJECTED, null);
            throw new RecordRejectedException("the fake mainframe refused " + draft.getLocalId(), errors);
        }
        String number = nextNumber(draft.getType());
        if (repliesToLose > 0) {
            repliesToLose--;
            record(draft, parentNumbers, Outcome.REPLY_LOST, number);
            throw new UploadOutcomeUnknownException("the fake reply was lost");
        }
        record(draft, parentNumbers, Outcome.ACCEPTED, number);
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

    /**
     * Counts the records the mainframe really holds, including those whose reply
     * was lost. This is how a test notices a duplicate.
     *
     * @return the number of records created
     */
    public synchronized int recordsCreated() {
        return (int) attempts.stream().filter(attempt -> attempt.assignedNumber() != null).count();
    }

    private void record(Draft draft, List<String> parentNumbers, Outcome outcome, String assignedNumber) {
        attempts.add(new Attempt(draft.getLocalId(), draft.getType(), List.copyOf(parentNumbers),
                outcome, assignedNumber));
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
