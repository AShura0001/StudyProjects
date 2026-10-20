package lavs.ui.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;

import lavs.ui.LavsApp;
import lavs.ui.Navigator;
import lavs.ui.service.LoanRecords;
import lavs.ui.service.NotSupportedYetException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Add a loan to a customer; on success the new loan's details open. */
public class AddLoanController {

    @FXML private BorderPane root;
    @FXML private Node content;
    @FXML private ProgressIndicator progress;
    @FXML private TextField principalField;
    @FXML private TextField startDateField;
    @FXML private TextField renewalDateField;
    @FXML private TextField rateTypeField;
    @FXML private TextField rateAmountField;
    @FXML private TextField rateFrequencyField;
    @FXML private TextField paymentAmountField;
    @FXML private TextField paymentFrequencyField;
    @FXML private Label noticeLabel;

    private final LoanRecords loanService = LavsApp.context().loans();
    private final Map<String, TextField> byKey = new LinkedHashMap<>();
    private String customerNumber;
    private Runnable onBack = () -> Navigator.show("customersearch");
    private boolean busy;

    @FXML
    private void initialize() {
        byKey.put("principal", principalField);
        byKey.put("startDate", startDateField);
        byKey.put("renewalDate", renewalDateField);
        byKey.put("rateType", rateTypeField);
        byKey.put("rateAmount", rateAmountField);
        byKey.put("rateFrequency", rateFrequencyField);
        byKey.put("paymentAmount", paymentAmountField);
        byKey.put("paymentFrequency", paymentFrequencyField);

        setShown(noticeLabel, false);
        setShown(progress, false);
        for (TextField field : byKey.values()) {
            field.textProperty().addListener((obs, old, now) -> field.getStyleClass().remove("field-error"));
        }
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerAccelerators(scene);
                Platform.runLater(principalField::requestFocus);
            }
        });
    }

    /** Called by the customer's page before this one is displayed. */
    public void open(String customerNumber, Runnable back) {
        this.customerNumber = customerNumber;
        this.onBack = back;
    }

    private boolean isDirty() {
        return byKey.values().stream().anyMatch(f -> !f.getText().isBlank());
    }

    @FXML
    private void onSubmit() {
        if (busy) {
            return;
        }
        LoanRecords.NewLoan form = new LoanRecords.NewLoan(
                principalField.getText().trim(), rateTypeField.getText().trim(),
                rateAmountField.getText().trim(), rateFrequencyField.getText().trim(),
                paymentAmountField.getText().trim(), paymentFrequencyField.getText().trim(),
                startDateField.getText().trim(), renewalDateField.getText().trim());
        String customer = customerNumber;

        Task<LoanRecords.Created> task = new Task<>() {
            @Override
            protected LoanRecords.Created call() {
                return loanService.createLoan(customer, form);
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false);
            LoanRecords.Created result = task.getValue();
            if (result.errors().isEmpty()) {
                Runnable back = () -> Navigator.<CustomerDetailsController>show("customerdetails",
                        c -> c.open(customer, onBack));
                Navigator.<LoanDetailsController>show("loandetails", l -> l.open(result.loanNumber(), back));
            } else {
                showFieldErrors(result.errors());
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable cause = task.getException();
            showNotice(cause instanceof NotSupportedYetException
                    ? cause.getMessage()
                    : "Could not reach the mainframe. Your details are still on screen.");
        });

        showNotice("");
        setBusy(true);
        Thread worker = new Thread(task, "loan-create");
        worker.setDaemon(true);
        worker.start();
    }

    private void showFieldErrors(Map<String, String> errors) {
        TextField first = null;
        for (String key : errors.keySet()) {
            TextField field = byKey.get(key);
            if (field != null) {
                field.getStyleClass().add("field-error");
                if (first == null) {
                    first = field;
                }
            }
        }
        showNotice(String.join("  ", errors.values()));
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
                "You have not added this loan yet. Discard what you typed?", ButtonType.YES, ButtonType.NO);
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
        alert.setHeaderText("Add loan");
        alert.setContentText("""
                Tab        Next field
                Ctrl+S     Add loan
                Esc        Cancel and go back
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
