package lavs.ui.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;

import lavs.ui.LavsApp;
import lavs.ui.component.Choices;
import lavs.ui.component.Inputs;
import lavs.ui.Navigator;
import lavs.business.model.CoBorrowerType;
import lavs.business.service.LoanAccountService;
import lavs.business.service.LoanService;
import lavs.ui.component.Format;
import mainframe.client.messages.loan.AddCoborrower;

import java.util.Map;
import java.util.Optional;

/** Add a co-borrower or guarantor to a loan; on success the loan's page reopens. */
public class AddCoBorrowerController {

    @FXML private BorderPane root;
    @FXML private Node content;
    @FXML private ProgressIndicator progress;
    @FXML private TextField coBorrowerNumberField;
    @FXML private ComboBox<String> typeField;
    @FXML private Label noticeLabel;

    private final LoanAccountService loanAccounts = LavsApp.context().loanAccounts();
    private String loanNumber;
    private Runnable onBack = () -> Navigator.show("customersearch");
    private Runnable onDone = onBack;
    private boolean busy;

    @FXML
    private void initialize() {
        Inputs.choices(typeField, Choices.CO_BORROWER_TYPES, Choices::label);
        setShown(noticeLabel, false);
        setShown(progress, false);
        for (Control field : new Control[] {coBorrowerNumberField, typeField}) {
            Inputs.onEdit(field, () -> field.getStyleClass().remove("field-error"));
        }
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerAccelerators(scene);
                Platform.runLater(coBorrowerNumberField::requestFocus);
            }
        });
    }

    /**
     * Called by the loan's page before this one is displayed.
     *
     * @param back what Back does; {@code done} runs after a successful add, so the loan reloads
     */
    public void open(String loanNumber, Runnable back, Runnable done) {
        this.loanNumber = loanNumber;
        this.onBack = back;
        this.onDone = done;
    }

    private boolean isDirty() {
        return !coBorrowerNumberField.getText().isBlank() || typeField.getValue() != null;
    }

    @FXML
    private void onSubmit() {
        if (busy) {
            return;
        }
        String number = coBorrowerNumberField.getText().trim().toUpperCase();
        String typeValue = Inputs.valueOf(typeField);
        Map<String, String> missing = new java.util.LinkedHashMap<>();
        if (number.isEmpty()) {
            missing.put("coBorrowerNumber", "Customer ID is required.");
        }
        if (typeValue.isEmpty()) {
            missing.put("type", "Choose Co-borrower or Guarantor.");
        }
        if (!missing.isEmpty()) {
            showFieldErrors(missing);
            return;
        }
        CoBorrowerType type = AddCoborrower.TYPE_GUARANTOR.equals(typeValue)
                ? CoBorrowerType.GUARANTOR : CoBorrowerType.COBORROWER;
        String loan = loanNumber;

        Task<lavs.business.model.CoBorrower> task = new Task<>() {
            @Override
            protected lavs.business.model.CoBorrower call() {
                return loanAccounts.addCoBorrower(loan, number, type);
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false);
            onDone.run();
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable error = task.getException();
            if (Format.isRuleError(error)) {
                showFieldErrors(Format.fieldErrors(error, Map.of(LoanService.CO_BORROWER_FIELD, "coBorrowerNumber")));
            } else {
                showNotice(Format.problem(error, "Could not reach the mainframe. Your details are still on screen."));
            }
        });

        showNotice("");
        setBusy(true);
        Thread worker = new Thread(task, "coborrower-add");
        worker.setDaemon(true);
        worker.start();
    }

    private void showFieldErrors(Map<String, String> errors) {
        Map<String, Control> byKey = Map.of("coBorrowerNumber", coBorrowerNumberField, "type", typeField);
        Control first = null;
        for (String key : errors.keySet()) {
            Control field = byKey.get(key);
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
    private void onBack() {
        if (!busy && confirmDiscard()) {
            onBack.run();
        }
    }

    @FXML
    private void onLogout() {
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
                "You have not added this person yet. Discard what you typed?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Unsaved details");
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
        alert.setHeaderText("Add co-borrower");
        alert.setContentText("""
                Tab        Next field
                Ctrl+S     Add
                Esc        Cancel and go back
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
