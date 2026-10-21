package lavs.ui.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;

import lavs.ui.LavsApp;
import lavs.ui.Navigator;
import lavs.ui.component.Format;
import lavs.ui.component.Inputs;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * What the small edit forms have in common: a card of fields with
 * Cancel and Save, field errors, a busy spinner, discard confirmation and
 * the usual shortcuts. A subclass registers its fields and says what Save does.
 */
public abstract class FormController {

    @FXML protected BorderPane root;
    @FXML protected Node content;
    @FXML protected ProgressIndicator progress;
    @FXML protected Label noticeLabel;

    protected Runnable onBack = () -> Navigator.show("customersearch");
    protected Runnable onDone = () -> Navigator.show("customersearch");

    private final Map<String, Control> fields = new LinkedHashMap<>();
    private final Map<String, String> baseline = new HashMap<>();
    private Control firstField;
    private boolean busy;

    /** Short name for messages, e.g. "employer". */
    protected abstract String what();

    /** Heading for the shortcut list, e.g. "Edit employer". */
    protected abstract String screenName();

    /** Called when Save is pressed and nothing else is running. */
    protected abstract void submitForm();

    protected final void register(String key, Control field) {
        fields.put(key, field);
    }

    /** Call once from the subclass's initialize(), after registering every field. */
    protected final void setUp(Control first) {
        firstField = first;
        setShown(noticeLabel, false);
        setShown(progress, false);
        for (Control field : fields.values()) {
            Inputs.onEdit(field, () -> field.getStyleClass().remove("field-error"));
        }
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerAccelerators(scene);
                Platform.runLater(firstField::requestFocus);
            }
        });
        markClean();
    }

    /** Remembers the current values as "nothing to lose" for the discard check. */
    protected final void markClean() {
        baseline.clear();
        fields.forEach((key, field) -> baseline.put(key, Inputs.valueOf(field)));
    }

    private boolean isDirty() {
        return fields.entrySet().stream().anyMatch(e -> !Inputs.valueOf(e.getValue()).equals(baseline.get(e.getKey())));
    }

    /** Runs a blocking read off the JavaFX thread and hands the result to {@code fill}. */
    protected final <T> void load(Callable<T> work, Consumer<T> fill) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            fill.accept(task.getValue());
            markClean();
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showNotice(Format.problem(task.getException(), "Could not reach the mainframe. Try again."));
        });
        setBusy(true);
        start(task, what() + "-load");
    }

    /**
     * How the business layer's field names map to this form's keys. By default a
     * business field matches the form key of the same name.
     */
    protected Map<String, String> businessFields() {
        Map<String, String> same = new HashMap<>();
        fields.keySet().forEach(key -> same.put(key, key));
        return same;
    }

    /**
     * Runs a blocking save. An empty error map means it worked and the form closes;
     * a business rule or validation error is shown on the matching fields.
     */
    protected final void save(Callable<Map<String, String>> work) {
        Task<Map<String, String>> task = new Task<>() {
            @Override
            protected Map<String, String> call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            Map<String, String> errors = task.getValue();
            if (errors.isEmpty()) {
                onDone.run();
            } else {
                showFieldErrors(errors);
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable error = task.getException();
            if (Format.isRuleError(error)) {
                showFieldErrors(Format.fieldErrors(error, businessFields()));
            } else {
                showNotice(Format.problem(error, "Could not reach the mainframe. Your changes are still on screen."));
            }
        });
        showNotice("");
        setBusy(true);
        start(task, what() + "-save");
    }

    private static void start(Task<?> task, String name) {
        Thread worker = new Thread(task, name);
        worker.setDaemon(true);
        worker.start();
    }

    private void showFieldErrors(Map<String, String> errors) {
        Control first = null;
        for (String key : errors.keySet()) {
            Control field = fields.get(key);
            if (field != null) {
                field.getStyleClass().add("field-error");
                if (first == null) {
                    first = field;
                }
            }
        }
        showNotice(Inputs.notice(errors));
        if (first != null) {
            first.requestFocus();
        }
    }

    @FXML
    protected void onSubmit() {
        if (busy) {
            return;
        }
        Map<String, String> badDates = Inputs.badDates(fields);
        if (badDates.isEmpty()) {
            submitForm();
        } else {
            showFieldErrors(badDates);
        }
    }

    @FXML
    protected void onBack() {
        if (!busy && confirmDiscard()) {
            onBack.run();
        }
    }

    @FXML
    protected void onLogout() {
        if (!busy && confirmDiscard()) {
            LavsApp.context().logout();
            Navigator.show("loggedout");
        }
    }

    private boolean confirmDiscard() {
        if (!isDirty()) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "You have not saved your changes to this " + what() + ". Discard them?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText(null);
        alert.initOwner(root.getScene().getWindow());
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.YES;
    }

    /** Each accelerator is a second route to a control that is already on screen. */
    private void registerAccelerators(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::onBack);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::onSubmit);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN), this::onLogout);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), this::showShortcuts);
    }

    private void setBusy(boolean value) {
        busy = value;
        setShown(progress, value);
        content.setOpacity(value ? 0.35 : 1.0);
        content.setDisable(value);
    }

    protected final void showNotice(String text) {
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
        alert.setHeaderText(screenName());
        alert.setContentText("""
                Tab        Next field
                Ctrl+S     Save
                Esc        Cancel and go back
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
