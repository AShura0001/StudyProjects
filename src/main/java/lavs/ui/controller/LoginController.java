package lavs.ui.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;

import lavs.business.model.AuthResult;
import lavs.business.service.AuthService;
import lavs.ui.Navigator;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the login screen.
 *
 * Holds no rules of its own: it collects input, hands it to the service, and
 * renders the result. The only checking done here is "is the field empty",
 * which is a binding concern rather than a business rule.
 */
public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML private VBox card;
    @FXML private TextField employeeIdField;
    @FXML private PasswordField passwordField;
    @FXML private Button submitButton;
    @FXML private ProgressIndicator progress;
    @FXML private Label messageLabel;

    private final AuthService authService;
    private final Runnable onLoggedIn;

    public LoginController(AuthService authService, Runnable onLoggedIn) {
        this.authService = Objects.requireNonNull(authService, "authService must not be null");
        this.onLoggedIn = Objects.requireNonNull(onLoggedIn, "onLoggedIn must not be null");
    }

    @FXML
    private void initialize() {
        // Enter submits from either field, matching the default button.
        submitButton.setDefaultButton(true);

        // Clear a stale error as soon as the user starts correcting it.
        employeeIdField.textProperty().addListener((obs, old, now) -> clearMessage());
        passwordField.textProperty().addListener((obs, old, now) -> clearMessage());

        // Accelerators are registered once the node is attached to a scene.
        card.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerAccelerators(scene);
                Platform.runLater(employeeIdField::requestFocus);
            }
        });
    }

    /**
     * Every accelerator here is a second route to a control that is already
     * visible and clickable -- nothing is keyboard-only.
     */
    private void registerAccelerators(Scene scene) {
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F1),
                this::showShortcuts);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN),
                employeeIdField::requestFocus);
    }

    @FXML
    private void onSubmit() {
        String employeeId = employeeIdField.getText();
        String password = passwordField.getText();

        if (employeeId.isBlank() || password.isEmpty()) {
            showMessage("Enter your Employee ID and Password.", true);
            (employeeId.isBlank() ? employeeIdField : passwordField).requestFocus();
            return;
        }

        Task<AuthResult> task = new Task<>() {
            @Override
            protected AuthResult call() {
                return authService.login(employeeId, password);
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);
            AuthResult result = task.getValue();
            if (result.success()) {
                onLoggedIn.run();
                Navigator.show("customersearch");
            } else {
                showMessage(result.message(), true);
                passwordField.clear();
                passwordField.requestFocus();
            }
        });

        task.setOnFailed(event -> {
            setBusy(false);
            LOGGER.log(Level.SEVERE, "Login failed unexpectedly", task.getException());
            showMessage("Could not reach the mainframe. Try again.", true);
        });

        setBusy(true);
        Thread worker = new Thread(task, "login");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Swaps the submit button for a spinner so a slow mainframe never looks
     * like a frozen window.
     */
    private void setBusy(boolean busy) {
        submitButton.setVisible(!busy);
        submitButton.setManaged(!busy);
        progress.setVisible(busy);
        progress.setManaged(busy);
        employeeIdField.setDisable(busy);
        passwordField.setDisable(busy);
    }

    private void showMessage(String text, boolean isError) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().setAll(isError ? "message-error" : "message-muted");
        setFieldError(employeeIdField, isError);
        setFieldError(passwordField, isError);
    }

    private void clearMessage() {
        messageLabel.setText("");
        setFieldError(employeeIdField, false);
        setFieldError(passwordField, false);
    }

    private void setFieldError(TextInputControl field, boolean isError) {
        field.getStyleClass().remove("field-error");
        if (isError) {
            field.getStyleClass().add("field-error");
        }
    }

    private void showShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard shortcuts");
        alert.setHeaderText("Login");
        alert.setContentText("""
                Enter      Sign in
                Tab        Next field
                Ctrl+L     Jump to Employee ID
                F1         This list""");
        alert.initOwner(card.getScene().getWindow());
        alert.showAndWait();
    }
}
