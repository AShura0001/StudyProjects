package lavs.ui.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import lavs.ui.Navigator;
import lavs.ui.stub.StubCustomerService;

/** Customer Details in view mode. Edit mode is a separate frame and is not built yet. */
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

    private final StubCustomerService customerService = new StubCustomerService(350);
    private Runnable onBack = () -> Navigator.show("customersearch");

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
     * Called by the previous screen before this one is displayed.
     *
     * @param back what Back should do, so the user returns to where they came from
     */
    public void open(String customerNumber, Runnable back) {
        onBack = back;
        customerIdLabel.setText(customerNumber);
        setLoading(true);

        Task<StubCustomerService.Details> task = new Task<>() {
            @Override
            protected StubCustomerService.Details call() {
                return customerService.details(customerNumber);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            StubCustomerService.Details details = task.getValue();
            if (details == null) {
                showNotice("Could not find customer " + customerNumber + ".");
            } else {
                populate(details);
            }
        });
        task.setOnFailed(e -> {
            setLoading(false);
            showNotice("Could not reach the mainframe. Try again.");
        });

        Thread worker = new Thread(task, "customer-details");
        worker.setDaemon(true);
        worker.start();
    }

    private void populate(StubCustomerService.Details d) {
        firstNameField.setText(d.firstName());
        lastNameField.setText(d.lastName());
        phoneField.setText(d.phone());
        emailField.setText(d.email());
        addressLine1Field.setText(d.addressLine1());
        addressLine2Field.setText(d.addressLine2());
        notesArea.setText(d.notes());

        employersBox.getChildren().clear();
        for (StubCustomerService.Employer employer : d.employers()) {
            employersBox.getChildren().add(listItem(employer.name(), employer.id(), null));
        }

        loansBox.getChildren().clear();
        for (StubCustomerService.Loan loan : d.loans()) {
            loansBox.getChildren().add(listItem(loan.name(), loan.date() + "  •  " + loan.loanNumber(),
                    () -> showNotice("The loan details screen has not been built yet.")));
        }
    }

    private VBox listItem(String title, String subtitle, Runnable onFurtherDetails) {
        VBox item = new VBox(1);
        item.getStyleClass().add("list-item");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("list-item-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("list-item-sub");
        item.getChildren().addAll(titleLabel, subtitleLabel);

        if (onFurtherDetails != null) {
            Hyperlink link = new Hyperlink("Click here for further details");
            link.getStyleClass().add("list-item-link");
            link.setOnAction(e -> onFurtherDetails.run());
            item.getChildren().add(link);
        }
        return item;
    }

    /** Each accelerator is a second route to a control that is already on screen. */
    private void registerAccelerators(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::onBack);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN), this::onLogout);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), this::showShortcuts);
    }

    @FXML
    private void onBack() {
        onBack.run();
    }

    @FXML
    private void onLogout() {
        Navigator.show("loggedout");
    }

    @FXML
    private void onEditMode() {
        showNotice("Edit mode is the next frame to be built.");
    }

    @FXML
    private void onAddLoan() {
        showNotice("Adding a loan has not been built yet.");
    }

    private void setLoading(boolean loading) {
        setShown(progress, loading);
        content.setOpacity(loading ? 0.35 : 1.0);
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
                Esc        Back
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
