package lavs.ui.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import lavs.business.model.LoanNote;
import lavs.business.repository.LoanNoteGateway;
import lavs.business.repository.PageResult;
import lavs.ui.LavsApp;
import lavs.ui.Navigator;
import lavs.ui.component.Format;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Every note on one loan, with a way to add, change and remove them. */
public class LoanNotesController {

    private static final long PAGE_SIZE = 50;
    static final String DEFAULT_TITLE = "General";

    @FXML private BorderPane root;
    @FXML private Node content;
    @FXML private ProgressIndicator progress;
    @FXML private Label loanIdLabel;
    @FXML private VBox notesBox;
    @FXML private Label noticeLabel;

    private final LoanNoteGateway notes = LavsApp.context().loanNotes();
    private String loanNumber;
    private Runnable onBack = () -> Navigator.show("customersearch");
    private boolean busy;

    @FXML
    private void initialize() {
        setShown(noticeLabel, false);
        setShown(progress, false);
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerAccelerators(scene);
                Platform.runLater(root::requestFocus);
            }
        });
    }

    /**
     * Called by the loan's page before this one is displayed.
     *
     * @param back what Back does, so the user returns to the loan
     */
    public void open(String loanNumber, Runnable back) {
        this.loanNumber = loanNumber;
        this.onBack = back;
        loanIdLabel.setText(loanNumber);
        reload("");
    }

    private void reload(String noticeWhenDone) {
        String loan = loanNumber;
        Task<List<LoanNote>> task = new Task<>() {
            @Override
            protected List<LoanNote> call() {
                // The list holds titles and dates only; each note's text is fetched on its own.
                PageResult<LoanNote> page = notes.listNotes(loan, 1, PAGE_SIZE);
                List<LoanNote> full = new ArrayList<>();
                for (LoanNote summary : page.getList()) {
                    full.add(notes.findNote(loan, summary.id()).orElse(summary));
                }
                return full;
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            render(task.getValue());
            showNotice(noticeWhenDone);
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showNotice(Format.problem(task.getException(), "Could not reach the mainframe. Try again."));
        });
        setBusy(true);
        startWorker(task, "loan-notes");
    }

    private void render(List<LoanNote> list) {
        notesBox.getChildren().clear();
        if (list.isEmpty()) {
            Label none = new Label("No notes on this loan yet.");
            none.getStyleClass().add("notes-empty");
            notesBox.getChildren().add(none);
            return;
        }
        for (LoanNote note : list) {
            notesBox.getChildren().add(card(note));
        }
    }

    private Node card(LoanNote note) {
        Label title = new Label(Format.text(note.type()).isEmpty() ? DEFAULT_TITLE : Format.text(note.type()));
        title.getStyleClass().add("note-card-title");
        title.setMinWidth(0);

        String who = Format.text(note.author());
        String when = Format.text(note.dateAdded());
        if (when.length() > 10) {
            when = when.substring(0, 10);
        }
        String meta = who.isEmpty() ? when : when.isEmpty() ? who : who + "  •  " + when;
        Label metaLabel = new Label(meta);
        metaLabel.getStyleClass().add("note-card-meta");
        metaLabel.setMinWidth(0);

        VBox heading = new VBox(1, title, metaLabel);
        HBox.setHgrow(heading, Priority.ALWAYS);
        heading.setMinWidth(0);

        Button edit = new Button("Edit");
        edit.getStyleClass().add("btn-edit-chip");
        edit.setMinWidth(Region.USE_PREF_SIZE);
        edit.setOnAction(e -> openEditor(note.id()));

        Button remove = new Button();
        remove.getStyleClass().add("btn-remove");
        SVGPath cross = new SVGPath();
        cross.setContent("M5 5 L19 19 M19 5 L5 19");
        cross.getStyleClass().add("remove-icon");
        remove.setGraphic(cross);
        remove.setMinWidth(Region.USE_PREF_SIZE);
        remove.setOnAction(e -> remove(note));

        HBox top = new HBox(6, heading, edit, remove);
        top.setAlignment(Pos.CENTER_LEFT);

        Label text = new Label(Format.text(note.lines()));
        text.getStyleClass().add("note-card-text");
        text.setWrapText(true);
        text.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(6, top, text);
        card.getStyleClass().add("note-card");
        return card;
    }

    private void remove(LoanNote note) {
        if (busy) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove this note from the loan? This cannot be undone.", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Remove note");
        alert.setHeaderText(null);
        alert.initOwner(root.getScene().getWindow());
        Optional<ButtonType> answer = alert.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.YES) {
            return;
        }

        String loan = loanNumber;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                notes.deleteNote(loan, note.id());
                return null;
            }
        };
        task.setOnSucceeded(e -> reload("Note removed."));
        task.setOnFailed(e -> {
            setBusy(false);
            showNotice(Format.problem(task.getException(), "Could not remove the note. Try again."));
        });
        showNotice("");
        setBusy(true);
        startWorker(task, "loan-note-remove");
    }

    private void openEditor(Long noteId) {
        String loan = loanNumber;
        Runnable back = onBack;
        Runnable reopen = () -> Navigator.<LoanNotesController>show("loannotes", n -> n.open(loan, back));
        Navigator.<EditLoanNoteController>show("editloannote", e -> e.open(loan, noteId, reopen, reopen));
    }

    @FXML
    private void onAddNote() {
        if (!busy && loanNumber != null) {
            openEditor(null);
        }
    }

    @FXML
    private void onBack() {
        if (!busy) {
            onBack.run();
        }
    }

    @FXML
    private void onLogout() {
        if (!busy) {
            LavsApp.context().logout();
            Navigator.show("loggedout");
        }
    }

    /** Each accelerator is a second route to a control that is already on screen. */
    private void registerAccelerators(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::onBack);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), this::onAddNote);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN), this::onLogout);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), this::showShortcuts);
    }

    private void setBusy(boolean value) {
        busy = value;
        setShown(progress, value);
        content.setOpacity(value ? 0.35 : 1.0);
        content.setDisable(value);
    }

    private static void startWorker(Task<?> task, String name) {
        Thread worker = new Thread(task, name);
        worker.setDaemon(true);
        worker.start();
    }

    private void showNotice(String text) {
        noticeLabel.setText(text);
        setShown(noticeLabel, !text.isEmpty());
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private void showShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard shortcuts");
        alert.setHeaderText("Loan notes");
        alert.setContentText("""
                Ctrl+N     Add a note
                Esc        Back to the loan
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
