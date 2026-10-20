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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import lavs.ui.Navigator;
import lavs.ui.LavsApp;
import lavs.ui.component.NotesPaper;
import lavs.ui.service.CustomerDirectory;
import lavs.ui.service.NotSupportedYetException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
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
    @FXML private StackPane photoCard;
    @FXML private StackPane avatarBox;

    private final CustomerDirectory customerService = LavsApp.context().customers();
    private Runnable onBack = () -> Navigator.show("customersearch");

    private CustomerDirectory.Details details;
    private List<CustomerDirectory.Employer> workingEmployers = new ArrayList<>();
    private boolean editing;
    private ImageView photoView;
    private byte[] savedPhoto;
    private byte[] pendingPhoto;
    private boolean busy;

    @FXML
    private void initialize() {
        NotesPaper.attach(notesArea);
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

        Task<CustomerDirectory.Details> task = new Task<>() {
            @Override
            protected CustomerDirectory.Details call() {
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
                loadPhoto(customerNumber);
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
        pendingPhoto = null;
        showPhoto(savedPhoto);
    }

    private boolean isDirty() {
        return details != null && (pendingPhoto != null || !details.equals(readForm()));
    }

    private CustomerDirectory.Details readForm() {
        return new CustomerDirectory.Details(details.customerNumber(),
                firstNameField.getText().trim(), lastNameField.getText().trim(),
                phoneField.getText().trim(), emailField.getText().trim(),
                addressLine1Field.getText().trim(), addressLine2Field.getText().trim(),
                notesArea.getText(), List.copyOf(workingEmployers), details.loans());
    }

    private void renderEmployers() {
        employersBox.getChildren().clear();
        for (CustomerDirectory.Employer employer : workingEmployers) {
            employersBox.getChildren().add(employerItem(employer));
        }
    }

    private void renderLoans() {
        loansBox.getChildren().clear();
        for (CustomerDirectory.Loan loan : details.loans()) {
            VBox item = listItem(loan.name(), loan.date() + "  •  " + loan.loanNumber());
            Hyperlink link = new Hyperlink("Click here for further details");
            link.getStyleClass().add("list-item-link");
            link.setOnAction(e -> openLoan(loan));
            item.getChildren().add(link);
            loansBox.getChildren().add(item);
        }
    }

    private void openLoan(CustomerDirectory.Loan loan) {
        if (!confirmDiscard()) {
            return;
        }
        String customerNumber = details.customerNumber();
        Runnable backFromCustomer = onBack;
        Navigator.<LoanDetailsController>show("loandetails", l -> l.open(loan.loanNumber(),
                () -> Navigator.<CustomerDetailsController>show("customerdetails",
                        c -> c.open(customerNumber, backFromCustomer))));
    }

    private void openEmployerEditor(CustomerDirectory.Employer employer) {
        if (!confirmDiscard()) {
            return;
        }
        String customerNumber = details.customerNumber();
        Runnable backFromCustomer = onBack;
        Runnable reopen = () -> Navigator.<CustomerDetailsController>show("customerdetails",
                c -> c.open(customerNumber, backFromCustomer));
        Navigator.<EditEmployerController>show("editemployer",
                e -> e.open(customerNumber, employer.id(), reopen, reopen));
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
    private Node employerItem(CustomerDirectory.Employer employer) {
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
        edit.setOnAction(e -> openEmployerEditor(employer));

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
        if (!busy && details != null && !editing) {
            showNotice("");
            applyMode(true);
        }
    }

    @FXML
    private void onViewMode() {
        if (!busy && editing && confirmDiscard()) {
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
        if (busy || !editing || details == null) {
            return;
        }
        CustomerDirectory.Details edited = readForm();
        byte[] photoToSave = pendingPhoto;
        clearFieldErrors();
        setBusy(true);

        Task<Map<String, String>> task = new Task<>() {
            @Override
            protected Map<String, String> call() {
                Map<String, String> errors = customerService.save(edited);
                if (errors.isEmpty() && photoToSave != null) {
                    customerService.savePhoto(edited.customerNumber(), photoToSave);
                }
                return errors;
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false);
            Map<String, String> errors = task.getValue();
            if (errors.isEmpty()) {
                details = edited;
                if (photoToSave != null) {
                    savedPhoto = photoToSave;
                }
                revert();
                applyMode(false);
                showNotice("Changes saved.");
            } else {
                showFieldErrors(errors);
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showNotice(e.getSource().getException() instanceof NotSupportedYetException
                    ? e.getSource().getException().getMessage()
                    : "Could not reach the mainframe. Your changes are still on screen.");
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
        errors.keySet().stream().map(fields::get).filter(java.util.Objects::nonNull).findFirst()
                .ifPresent(TextField::requestFocus);
    }

    private void clearFieldErrors() {
        for (TextField field : List.of(firstNameField, lastNameField, phoneField, emailField,
                addressLine1Field, addressLine2Field)) {
            toggleStyle(field, "field-error", false);
        }
    }

    @FXML
    private void onPhotoEdit() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a photo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        java.io.File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            if (file.length() > 5_000_000) {
                showNotice("That photo is larger than 5 MB. Choose a smaller one.");
                return;
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            if (new Image(new ByteArrayInputStream(bytes)).isError()) {
                showNotice("That file could not be read as an image.");
                return;
            }
            showNotice("");
            pendingPhoto = bytes;
            showPhoto(bytes);
        } catch (IOException e) {
            showNotice("That file could not be read.");
        }
    }

    /** Shows the given image in the photo card, or the placeholder person when there is none. */
    private void showPhoto(byte[] bytes) {
        if (bytes == null) {
            if (photoView != null) {
                photoView.setVisible(false);
            }
            avatarBox.setVisible(true);
            return;
        }
        if (photoView == null) {
            photoView = new ImageView();
            photoView.setPreserveRatio(true);
            photoView.setFitWidth(150);
            photoView.setFitHeight(150);
            photoView.setMouseTransparent(true);
            photoCard.getChildren().add(1, photoView);
        }
        Image image = new Image(new ByteArrayInputStream(bytes), 150, 150, true, true);
        photoView.setImage(image);
        Rectangle round = new Rectangle(image.getWidth(), image.getHeight());
        round.setArcWidth(16);
        round.setArcHeight(16);
        photoView.setClip(round);
        photoView.setVisible(true);
        avatarBox.setVisible(false);
    }

    private void loadPhoto(String customerNumber) {
        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() {
                return customerService.photo(customerNumber);
            }
        };
        task.setOnSucceeded(e -> {
            savedPhoto = task.getValue();
            if (pendingPhoto == null) {
                showPhoto(savedPhoto);
            }
        });
        startWorker(task, "customer-photo");
    }

    @FXML
    private void onAddLoan() {
        if (details == null || !confirmDiscard()) {
            return;
        }
        String customerNumber = details.customerNumber();
        Runnable backFromCustomer = onBack;
        Navigator.<AddLoanController>show("addloan", a -> a.open(customerNumber,
                () -> Navigator.<CustomerDetailsController>show("customerdetails",
                        c -> c.open(customerNumber, backFromCustomer))));
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
            LavsApp.context().logout();
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
        this.busy = busy;
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
