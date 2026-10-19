package lavs.business.repository;

import lavs.business.model.LoanNote;

import java.util.Optional;

/**
 * Reads and changes the notes on a loan in the mainframe.
 *
 * <p>Every method throws {@link MainframeUnavailableException} if the mainframe
 * could not be reached.
 */
public interface LoanNoteGateway {

    /**
     * Gets one note on a loan, including its text.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param id the mainframe's id for the note on the loan
     * @return the note, or empty if there is no such note
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    Optional<LoanNote> findNote(String loanNumber, long id);

    /**
     * Gets one page of the notes on a loan. The mainframe does not send the text
     * of each note in a list, so the lines are null.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param page the page to get, starting at 1
     * @param pageSize how many notes to put on a page
     * @return the page; empty if the loan has none or does not exist
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    PageResult<LoanNote> listNotes(String loanNumber, long page, long pageSize);

    /**
     * Adds a note to a loan.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param type the heading of the note
     * @param lines the text of the note
     * @return the note as the mainframe saved it
     * @throws RecordRejectedException if the mainframe refused the note
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    LoanNote addNote(String loanNumber, String type, String lines);

    /**
     * Changes the heading and text of a note.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param id the mainframe's id for the note on the loan
     * @param type the new heading
     * @param lines the new text
     * @return the note as the mainframe saved it
     * @throws RecordRejectedException if the mainframe refused the change
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    LoanNote updateNote(String loanNumber, long id, String type, String lines);

    /**
     * Removes a note from a loan.
     *
     * @param loanNumber the mainframe's number for the loan
     * @param id the mainframe's id for the note on the loan
     * @throws RecordRejectedException if the mainframe refused, for example because there is no such note
     * @throws MainframeUnavailableException if the mainframe could not be reached
     */
    void deleteNote(String loanNumber, long id);
}
