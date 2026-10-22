package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import lavs.business.model.Note;
import lavs.business.service.CustomerService;
import lavs.ui.LavsApp;
import lavs.ui.component.Inputs;

import java.util.Map;

/** Adds a note to a customer, or changes one, through CustomerService. On save the customer's page reopens. */
public class EditNoteController extends FormController {

    @FXML private Label titleLabel;
    @FXML private Label countLabel;
    @FXML private TextField typeField;
    @FXML private TextArea linesField;

    private final CustomerService customers = LavsApp.context().customers();
    private String customerNumber;
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
        countLabel.setText(used + " / " + CustomerService.MAX_NOTE_LENGTH);
        countLabel.getStyleClass().remove("over-limit");
        if (used > CustomerService.MAX_NOTE_LENGTH) {
            countLabel.getStyleClass().add("over-limit");
        }
    }

    /**
     * Called by the customer's page before this one is displayed.
     *
     * @param noteId the note to change, or null to add a new one
     * @param back what Back does; {@code done} runs after a successful save
     */
    public void open(String customerNumber, Long noteId, Runnable back, Runnable done) {
        this.customerNumber = customerNumber;
        this.noteId = noteId;
        this.onBack = back;
        this.onDone = done;
        if (noteId == null) {
            titleLabel.setText("Add a note");
            markClean();
            return;
        }
        titleLabel.setText("Edit note");
        load(() -> customers.findNote(customerNumber, noteId).orElse(null), note -> {
            if (note == null) {
                showNotice("Could not find that note.");
                return;
            }
            Inputs.setValue(typeField, note.get_type());
            linesField.setText(note.get_lines() == null ? "" : note.get_lines());
        });
    }

    @Override
    protected void submitForm() {
        Note note = new Note();
        note.set_customerNumber(customerNumber);
        note.set_type(Inputs.valueOf(typeField));
        note.set_lines(linesField.getText());
        Long id = noteId;
        save(() -> {
            if (id == null) {
                customers.addNote(note);
            } else {
                note.set_id(id);
                customers.updateNote(note);
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
