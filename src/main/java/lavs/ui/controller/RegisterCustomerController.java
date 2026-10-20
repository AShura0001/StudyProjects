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
import lavs.ui.service.CustomerDirectory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Register a new customer; on success the new customer's details screen opens. */
public class RegisterCustomerController {

    @FXML private BorderPane root;
    @FXML private Node content;
    @FXML private ProgressIndicator progress;
    @FXML private TextField titleField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField dateOfBirthField;
    @FXML private TextField nationalityField;
    @FXML private Label noticeLabel;

    private final CustomerDirectory customerService = LavsApp.context().customers();
    private boolean busy;

    @FXML
    private void initialize() {
        setShown(noticeLabel, false);
        setShown(progress, false);
        for (TextField field : fields()) {
            field.textProperty().addListener((obs, old, now) -> field.getStyleClass().remove("field-error"));
        }
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerAccelerators(scene);
                Platform.runLater(titleField::requestFocus);
            }
        });
    }

    private List<TextField> fields() {
        return List.of(titleField, firstNameField, lastNameField, dateOfBirthField, nationalityField);
    }

    private boolean isDirty() {
        return fields().stream().anyMatch(f -> !f.getText().isBlank());
    }

    @FXML
    private void onRegister() {
        if (busy) {
            return;
        }
        CustomerDirectory.NewCustomer form = new CustomerDirectory.NewCustomer(
                titleField.getText().trim(), firstNameField.getText().trim(),
                lastNameField.getText().trim(), dateOfBirthField.getText().trim(),
                nationalityField.getText().trim());

        Task<CustomerDirectory.Registration> task = new Task<>() {
            @Override
            protected CustomerDirectory.Registration call() {
                return customerService.register(form);
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false);
            CustomerDirectory.Registration result = task.getValue();
            if (result.errors().isEmpty()) {
                Navigator.<CustomerDetailsController>show("customerdetails",
                        c -> c.open(result.customerNumber(), () -> Navigator.show("customersearch")));
            } else {
                showFieldErrors(result.errors());
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showNotice("Could not reach the mainframe. Your details are still on screen.");
        });

        showNotice("");
        setBusy(true);
        Thread worker = new Thread(task, "customer-register");
        worker.setDaemon(true);
        worker.start();
    }

    private void showFieldErrors(Map<String, String> errors) {
        Map<String, TextField> byKey = Map.of(
                "title", titleField, "firstName", firstNameField, "lastName", lastNameField,
                "dateOfBirth", dateOfBirthField, "nationality", nationalityField);
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
            Navigator.show("customersearch");
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
                "You have not registered this customer yet. Discard what you typed?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Unsaved details");
        alert.setHeaderText(null);
        alert.initOwner(root.getScene().getWindow());
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.YES;
    }

    /** Each accelerator is a second route to a control that is already on screen. */
    private void registerAccelerators(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::onBack);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::onRegister);
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
        alert.setHeaderText("Register customer");
        alert.setContentText("""
                Tab        Next field
                Ctrl+S     Register
                Esc        Cancel and go back
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
