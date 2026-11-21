package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import lavs.business.service.LoanNoteService;
import lavs.ui.LavsApp;

import java.util.Map;

/**
 * Adds a note to a loan, or changes one. On save the loan's notes reopen. The rules
 * (some text, at most {@link LoanNoteService#MAX_LENGTH} characters) are checked by
 * {@link LoanNoteService}; problems come back against the fields.
 */
public class EditLoanNoteController extends FormController {

    private static final int MAX_LENGTH = LoanNoteService.MAX_LENGTH;

    @FXML private Label titleLabel;
    @FXML private Label countLabel;
    @FXML private TextField typeField;
    @FXML private TextArea linesField;

    private final LoanNoteService notes = LavsApp.context().loanNotes();
    private String loanNumber;
    /** The note being changed, or null when adding one. */
    private Long noteId;

    @FXML
    private void initialize() {
        register("type", typeField);
        register("lines", linesField);
        linesField.textProperty().addListener((obs, old, now) -> showCount());
        setUp(linesField);
        showCount();
    }

    private void showCount() {
        int used = linesField.getText() == null ? 0 : linesField.getText().strip().length();
        countLabel.setText(used + " / " + MAX_LENGTH);
        countLabel.getStyleClass().remove("over-limit");
        if (used > MAX_LENGTH) {
            countLabel.getStyleClass().add("over-limit");
        }
    }

    /**
     * Called by the loan's notes before this one is displayed.
     *
     * @param noteId the note to change, or null to add a new one
     * @param back what Back does; {@code done} runs after a successful save
     */
    public void open(String loanNumber, Long noteId, Runnable back, Runnable done) {
        this.loanNumber = loanNumber;
        this.noteId = noteId;
        this.onBack = back;
        this.onDone = done;
        if (noteId == null) {
            titleLabel.setText("Add a note");
            markClean();
            return;
        }
        titleLabel.setText("Edit note");
        load(() -> notes.find(loanNumber, noteId).orElse(null), note -> {
            if (note == null) {
                showNotice("Could not find that note.");
                return;
            }
            typeField.setText(note.type() == null ? "" : note.type());
            linesField.setText(note.lines() == null ? "" : note.lines());
        });
    }

    @Override
    protected void submitForm() {
        String title = typeField.getText();
        String lines = linesField.getText();
        String loan = loanNumber;
        Long id = noteId;
        save(() -> {
            if (id == null) {
                notes.add(loan, title, lines);
            } else {
                notes.update(loan, id, title, lines);
            }
            return Map.of();
        });
    }

    @Override
    protected String what() {
        return "note";
    }

    @Override
    protected String screenName() {
        return noteId == null ? "Add note" : "Edit note";
    }
}
