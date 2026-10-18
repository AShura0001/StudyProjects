package lavs.business.model;

import java.util.Objects;

/**
 * A note on a loan, as the mainframe holds it.
 *
 * <p>When the note comes from a list, the mainframe does not send its text, so
 * the lines are null.
 *
 * @param id the mainframe's id for the note on the loan
 * @param loanNumber the mainframe's number for the loan
 * @param type the heading of the note
 * @param author the user who wrote the note; null if not sent
 * @param dateAdded when the note was added, as the mainframe sends it; null if not sent
 * @param lines the text of the note, one line per line break; null if not sent
 */
public record LoanNote(
        long id,
        String loanNumber,
        String type,
        String author,
        String dateAdded,
        String lines) {

    /**
     * Creates the note.
     *
     * @throws NullPointerException if the loan number or type is null
     */
    public LoanNote {
        Objects.requireNonNull(loanNumber, "loanNumber must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }
}
