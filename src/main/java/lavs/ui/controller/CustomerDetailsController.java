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
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import lavs.ui.Navigator;
import lavs.ui.stub.StubCustomerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Customer Details with a view mode and an edit mode. Edit changes are held in
 * the form until Save; Cancel (or leaving) discards them after confirmation.
 */
public class CustomerDetailsController {

    @FXML private BorderPane root;
    @FXML private Node content;
    @FXML private ProgressIndicator progress;
    @FXML private Label customerIdLabel;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressLine1Field;
    @FXML private TextField addressLine2Field;
    @FXML private TextArea notesArea;
    @FXML private VBox employersBox;
    @FXML private VBox loansBox;
    @FXML private Label noticeLabel;
    @FXML private Button viewModeButton;
    @FXML private Button editModeButton;
    @FXML private Button photoEditButton;
    @FXML private Node editActions;

    private final StubCustomerService customerService = new StubCustomerService(350);
    private Runnable onBack = () -> Navigator.show("customersearch");

    private StubCustomerService.Details details;
    private List<StubCustomerService.Employer> workingEmployers = new ArrayList<>();
    private boolean editing;

    @FXML
    private void initialize() {
        setShown(noticeLabel, false);
        setShown(progress, false);
        applyMode(false);
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerAccelerators(scene);
                Platform.runLater(root::requestFocus);
            }
        });
    }

    /**
     * Called by the previous screen before this one is displayed.
     *
     * @param back what Back should do, so the user returns to where they came from
     */
    public void open(String customerNumber, Runnable back) {
        onBack = back;
        customerIdLabel.setText(customerNumber);
        setBusy(true);

        Task<StubCustomerService.Details> task = new Task<>() {
            @Override
            protected StubCustomerService.Details call() {
                return customerService.details(customerNumber);
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false);
            details = task.getValue();
            if (details == null) {
                showNotice("Could not find customer " + customerNumber + ".");
            } else {
                revert();
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showNotice("Could not reach the mainframe. Try again.");
        });

        startWorker(task, "customer-details");
    }

    /** Puts the form, notes and employer list back to the last saved values. */
    private void revert() {
        firstNameField.setText(details.firstName());
        lastNameField.setText(details.lastName());
        phoneField.setText(details.phone());
        emailField.setText(details.email());
        addressLine1Field.setText(details.addressLine1());
        addressLine2Field.setText(details.addressLine2());
        notesArea.setText(details.notes());
        workingEmployers = new ArrayList<>(details.employers());
        clearFieldErrors();
        renderEmployers();
        renderLoans();
    }

    private boolean isDirty() {
        return details != null && !details.equals(readForm());
    }

    private StubCustomerService.Details readForm() {
        return new StubCustomerService.Details(details.customerNumber(),
                firstNameField.getText().trim(), lastNameField.getText().trim(),
                phoneField.getText().trim(), emailField.getText().trim(),
                addressLine1Field.getText().trim(), addressLine2Field.getText().trim(),
                notesArea.getText(), List.copyOf(workingEmployers), details.loans());
    }

    private void renderEmployers() {
        employersBox.getChildren().clear();
        for (StubCustomerService.Employer employer : workingEmployers) {
            employersBox.getChildren().add(employerItem(employer));
        }
    }

    private void renderLoans() {
        loansBox.getChildren().clear();
        for (StubCustomerService.Loan loan : details.loans()) {
            VBox item = listItem(loan.name(), loan.date() + "  •  " + loan.loanNumber());
            Hyperlink link = new Hyperlink("Click here for further details");
            link.getStyleClass().add("list-item-link");
            link.setOnAction(e -> showNotice("The loan details screen has not been built yet."));
            item.getChildren().add(link);
            loansBox.getChildren().add(item);
        }
    }

    private VBox listItem(String title, String subtitle) {
        VBox item = new VBox(1);
        item.getStyleClass().add("list-item");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("list-item-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("list-item-sub");
        item.getChildren().addAll(titleLabel, subtitleLabel);
        return item;
    }

    /** In edit mode each employer row gains a remove and an Edit button. */
    private Node employerItem(StubCustomerService.Employer employer) {
        VBox text = listItem(employer.name(), employer.id());
        if (!editing) {
            return text;
        }

        Button remove = new Button();
        remove.getStyleClass().add("btn-remove");
        SVGPath cross = new SVGPath();
        cross.setContent("M5 5 L19 19 M19 5 L5 19");
        cross.getStyleClass().add("remove-icon");
        remove.setGraphic(cross);
        remove.setOnAction(e -> {
            workingEmployers.remove(employer);
            renderEmployers();
        });

        Button edit = new Button("Edit");
        edit.getStyleClass().add("btn-edit-chip");
        edit.setOnAction(e -> showNotice("Editing an employer has not been built yet."));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(6, text, spacer, remove, edit);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-row");
        return row;
    }

    /** Switches every editable part of the screen between view and edit mode. */
    private void applyMode(boolean edit) {
        editing = edit;
        for (TextField field : List.of(firstNameField, lastNameField, phoneField, emailField,
                addressLine1Field, addressLine2Field)) {
            field.setEditable(edit);
            field.setFocusTraversable(edit);
        }
        notesArea.setEditable(edit);
        notesArea.setFocusTraversable(edit);

        toggleStyle(content, "editing", edit);
        toggleStyle(viewModeButton, "mode-selected", !edit);
        toggleStyle(editModeButton, "mode-selected", edit);
        setShown(photoEditButton, edit);
        setShown(editActions, edit);
        clearFieldErrors();
        if (details != null) {
            renderEmployers();
        }
        if (edit) {
            Platform.runLater(firstNameField::requestFocus);
        }
    }

    private static void toggleStyle(Node node, String styleClass, boolean on) {
        node.getStyleClass().remove(styleClass);
        if (on) {
            node.getStyleClass().add(styleClass);
        }
    }

    @FXML
    private void onEditMode() {
        if (details != null && !editing) {
            showNotice("");
            applyMode(true);
        }
    }

    @FXML
    private void onViewMode() {
        if (editing && confirmDiscard()) {
            revert();
            applyMode(false);
        }
    }

    @FXML
    private void onCancel() {
        onViewMode();
    }

    @FXML
    private void onSave() {
        if (!editing || details == null) {
            return;
        }
        StubCustomerService.Details edited = readForm();
        clearFieldErrors();
        setBusy(true);

        Task<Map<String, String>> task = new Task<>() {
            @Override
            protected Map<String, String> call() {
                return customerService.save(edited);
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false);
            Map<String, String> errors = task.getValue();
            if (errors.isEmpty()) {
                details = edited;
                revert();
                applyMode(false);
                showNotice("Changes saved.");
            } else {
                showFieldErrors(errors);
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showNotice("Could not reach the mainframe. Your changes are still on screen.");
        });

        startWorker(task, "customer-save");
    }

    private void showFieldErrors(Map<String, String> errors) {
        Map<String, TextField> fields = Map.of(
                "firstName", firstNameField, "lastName", lastNameField, "phone", phoneField,
                "email", emailField, "addressLine1", addressLine1Field);
        for (String key : errors.keySet()) {
            TextField field = fields.get(key);
            if (field != null) {
                toggleStyle(field, "field-error", true);
            }
        }
        showNotice(String.join("  ", errors.values()));
        fields.get(errors.keySet().iterator().next()).requestFocus();
    }

    private void clearFieldErrors() {
        for (TextField field : List.of(firstNameField, lastNameField, phoneField, emailField,
                addressLine1Field, addressLine2Field)) {
            toggleStyle(field, "field-error", false);
        }
    }

    @FXML
    private void onPhotoEdit() {
        showNotice("Changing the photo has not been built yet.");
    }

    @FXML
    private void onAddLoan() {
        showNotice("Adding a loan has not been built yet.");
    }

    @FXML
    private void onBack() {
        if (confirmDiscard()) {
            onBack.run();
        }
    }

    @FXML
    private void onLogout() {
        if (confirmDiscard()) {
            Navigator.show("loggedout");
        }
    }

    /** Esc cancels an edit in progress; otherwise it goes back. */
    private void onEscape() {
        if (editing) {
            onCancel();
        } else {
            onBack();
        }
    }

    /** True when there is nothing to lose or the user agrees to discard their edits. */
    private boolean confirmDiscard() {
        if (!editing || !isDirty()) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "You have unsaved changes. Discard them?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText(null);
        alert.initOwner(root.getScene().getWindow());
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.YES;
    }

    /** Each accelerator is a second route to a control that is already on screen. */
    private void registerAccelerators(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::onEscape);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN), this::onEditMode);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::onSave);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN), this::onLogout);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), this::showShortcuts);
    }

    private void setBusy(boolean busy) {
        setShown(progress, busy);
        content.setOpacity(busy ? 0.35 : 1.0);
        content.setDisable(busy);
    }

    private void startWorker(Task<?> task, String name) {
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
        alert.setHeaderText("Customer details");
        alert.setContentText("""
                Ctrl+E     Edit mode
                Ctrl+S     Save (in edit mode)
                Esc        Cancel editing, or go back
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
