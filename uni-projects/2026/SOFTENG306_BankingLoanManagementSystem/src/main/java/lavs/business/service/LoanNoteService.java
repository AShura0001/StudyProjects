package lavs.business.service;

import lavs.business.model.LoanNote;
import lavs.business.repository.LoanNoteGateway;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.PageResult;
import lavs.business.repository.SessionExpiredException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The rules for notes on a loan that is already on the mainframe: reading them, and
 * adding, changing and removing them.
 *
 * <p>The notes live on the mainframe, so every call here needs a connection, and a
 * staff member gets a clear message when there is none. A note needs some text and
 * can be at most {@link #MAX_LENGTH} characters long. A note without a title is
 * called {@value #DEFAULT_TITLE}, the same as a customer's note.
 *
 * <p>These calls contact the mainframe and can take a while, so a screen must call
 * them off its own thread.
 */
public class LoanNoteService {

    /** The title a note gets when none is given. */
    public static final String DEFAULT_TITLE = CustomerService.DEFAULT_NOTE_TYPE;

    /** Longest note text allowed, in characters, the same as for a customer's note. */
    public static final int MAX_LENGTH = CustomerService.MAX_NOTE_LENGTH;

    /** Longest title allowed, in characters. */
    public static final int MAX_TITLE_LENGTH = 40;

    /** The field name used for a problem with the text of a note. */
    public static final String LINES_FIELD = "lines";

    /** The field name used for a problem with the title of a note. */
    public static final String TITLE_FIELD = "type";

    /** Shown when the mainframe cannot be reached. */
    public static final String OFFLINE_MESSAGE =
            "Loan notes need a connection to the mainframe. Please try again when you are back online.";

    private static final long PAGE_SIZE = 100;
    private static final int MAX_PAGES = 1000;

    private final LoanNoteGateway notes;

    /**
     * Creates the service.
     *
     * @param notes how notes are read and changed on the mainframe
     * @throws NullPointerException if the gateway is null
     */
    public LoanNoteService(LoanNoteGateway notes) {
        this.notes = Objects.requireNonNull(notes, "notes must not be null");
    }

    /**
     * Gets every note on a loan, each with its text. The mainframe's list leaves the
     * text out, so each note is read on its own; if one cannot be read, the note is
     * still shown with the title and date the list gave.
     *
     * @param loanNumber the mainframe's number for the loan
     * @return the notes, in the order the mainframe lists them; empty if there are none
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    public List<LoanNote> notesOf(String loanNumber) {
        requireText(loanNumber, "loanNumber");
        return offline(() -> {
            List<LoanNote> all = new ArrayList<>();
            for (long page = 1; page <= MAX_PAGES; page++) {
                PageResult<LoanNote> result = notes.listNotes(loanNumber, page, PAGE_SIZE);
                for (LoanNote summary : result.getList()) {
                    all.add(notes.findNote(loanNumber, summary.id()).orElse(summary));
                }
                if (result.getList().isEmpty() || all.size() >= result.getTotal()) {
                    break;
                }
            }
            return all;
        });
    }

    /**
     * Gets one note on a loan, with its text.
     *
     * @return the note, or empty if there is no such note
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    public Optional<LoanNote> find(String loanNumber, long noteId) {
        requireText(loanNumber, "loanNumber");
        return offline(() -> notes.findNote(loanNumber, noteId));
    }

    /**
     * Adds a note to a loan. Leading and trailing spaces are removed first.
     *
     * @param title the heading; blank means {@value #DEFAULT_TITLE}
     * @param lines the text of the note
     * @return the note as the mainframe saved it
     * @throws ValidationException if the text is empty or too long, or the title is too long
     * @throws lavs.business.repository.RecordRejectedException if the mainframe refused the note
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    public LoanNote add(String loanNumber, String title, String lines) {
        requireText(loanNumber, "loanNumber");
        String cleanTitle = titleOf(title);
        String cleanLines = linesOf(lines);
        check(cleanTitle, cleanLines);
        return offline(() -> notes.addNote(loanNumber, cleanTitle, cleanLines));
    }

    /**
     * Changes the title and text of a note, with the same rules as {@link #add}.
     *
     * @return the note as the mainframe now holds it
     * @throws ValidationException if the text is empty or too long, or the title is too long
     * @throws lavs.business.repository.RecordRejectedException if the mainframe refused the change
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    public LoanNote update(String loanNumber, long noteId, String title, String lines) {
        requireText(loanNumber, "loanNumber");
        String cleanTitle = titleOf(title);
        String cleanLines = linesOf(lines);
        check(cleanTitle, cleanLines);
        return offline(() -> notes.updateNote(loanNumber, noteId, cleanTitle, cleanLines));
    }

    /**
     * Removes a note from a loan.
     *
     * @throws lavs.business.repository.RecordRejectedException if the mainframe refused, for
     *                                                          example because there is no such note
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    public void delete(String loanNumber, long noteId) {
        requireText(loanNumber, "loanNumber");
        offline(() -> {
            notes.deleteNote(loanNumber, noteId);
            return null;
        });
    }

    private static String titleOf(String title) {
        String clean = title == null ? "" : title.strip();
        return clean.isEmpty() ? DEFAULT_TITLE : clean;
    }

    private static String linesOf(String lines) {
        return lines == null ? "" : lines.strip();
    }

    /** Reports every problem at once, by the field it belongs to. */
    private static void check(String title, String lines) {
        List<ValidationError> errors = new ArrayList<>();
        if (lines.isEmpty()) {
            errors.add(new ValidationError(LINES_FIELD, "Write something in the note"));
        } else if (lines.length() > MAX_LENGTH) {
            errors.add(new ValidationError(LINES_FIELD, "A note can be at most " + MAX_LENGTH + " characters"));
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            errors.add(new ValidationError(TITLE_FIELD, "The title can be at most " + MAX_TITLE_LENGTH + " characters"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /**
     * Gives every "cannot reach the mainframe" the same message staff can act on. An
     * ended session keeps its own message, since only logging in again fixes it.
     */
    private static <T> T offline(Supplier<T> call) {
        try {
            return call.get();
        } catch (SessionExpiredException e) {
            throw e;
        } catch (MainframeUnavailableException e) {
            throw new MainframeUnavailableException(OFFLINE_MESSAGE, e);
        }
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
