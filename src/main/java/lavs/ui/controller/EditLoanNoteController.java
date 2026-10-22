package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import lavs.business.model.LoanNote;
import lavs.business.repository.LoanNoteGateway;
import lavs.ui.LavsApp;

import java.util.Map;

/** Adds a note to a loan, or changes one. On save the loan's notes reopen. */
public class EditLoanNoteController extends FormController {

    private static final int MAX_LENGTH = 2000;

    @FXML private Label titleLabel;
    @FXML private Label countLabel;
    @FXML private TextField typeField;
    @FXML private TextArea linesField;

    private final LoanNoteGateway notes = LavsApp.context().loanNotes();
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
        load(() -> notes.findNote(loanNumber, noteId).orElse(null), note -> {
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
        String type = typeField.getText() == null ? "" : typeField.getText().strip();
        String lines = linesField.getText() == null ? "" : linesField.getText().strip();
        String title = type.isEmpty() ? LoanNotesController.DEFAULT_TITLE : type;
        Long id = noteId;
        save(() -> {
            if (lines.isEmpty()) {
                return Map.of("lines", "Write something in the note first.");
            }
            if (lines.length() > MAX_LENGTH) {
                return Map.of("lines", "A note can be at most " + MAX_LENGTH + " characters.");
            }
            LoanNote saved = id == null
                    ? notes.addNote(loanNumber, title, lines)
                    : notes.updateNote(loanNumber, id, title, lines);
            return saved == null ? Map.of("form", "The mainframe did not accept the note.") : Map.of();
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
