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

import lavs.business.model.PhoneNumber;
import lavs.business.service.CustomerService;
import lavs.ui.LavsApp;
import lavs.ui.Navigator;
import lavs.ui.component.Format;
import lavs.ui.component.Inputs;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Every phone number of one customer, read and changed through CustomerService.
 * Numbers are added and changed on their own form; a removal is confirmed and sent at once.
 */
public class ContactsController {

    /** One phone number: the number with its code, its type, and its flags. */
    private record PhoneRow(long id, String number, String type, boolean canText, boolean primary) {
    }

    @FXML private BorderPane root;
    @FXML private Node content;
    @FXML private ProgressIndicator progress;
    @FXML private Label customerLabel;
    @FXML private VBox phonesBox;
    @FXML private Label noticeLabel;

    private final CustomerService customers = LavsApp.context().customers();
    private Runnable onBack = () -> Navigator.show("customersearch");
    private String customerNumber;
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
     * Called by the customer's page before this one is displayed.
     *
     * @param back what Back does, so the user returns to the customer
     */
    public void open(String customerNumber, Runnable back) {
        this.customerNumber = customerNumber;
        this.onBack = back;
        customerLabel.setText("Customer " + customerNumber);
        load("");
    }

    private void load(String noticeWhenDone) {
        run(() -> customers.listPhones(customerNumber).stream()
                .map(ContactsController::phoneRow)
                .sorted(Comparator.comparing((PhoneRow p) -> !p.primary()))
                .toList(), phones -> {
            renderPhones(phones);
            showNotice(noticeWhenDone);
        }, "Could not reach the mainframe. Try again.");
    }

    private static PhoneRow phoneRow(PhoneNumber phone) {
        String type = Format.text(phone.get_type());
        if ("Other".equalsIgnoreCase(type) && !Format.text(phone.get_otherType()).isEmpty()) {
            type = Format.text(phone.get_otherType());
        }
        String number = (Format.text(phone.get_countryCode()) + " " + Format.text(phone.get_number())).trim();
        return new PhoneRow(phone.get_id(), number.isEmpty() ? "(no number)" : number, type,
                phone.is_canTxt(), phone.is_isPrimary());
    }

    private void renderPhones(List<PhoneRow> phones) {
        phonesBox.getChildren().clear();
        if (phones.isEmpty()) {
            Label none = new Label("No phone numbers yet.");
            none.getStyleClass().add("list-item-sub");
            phonesBox.getChildren().add(none);
        }
        for (PhoneRow phone : phones) {
            phonesBox.getChildren().add(phoneItem(phone));
        }
    }

    /** The number, then its type with a Primary tag, then remove and Edit. */
    private Node phoneItem(PhoneRow phone) {
        Label number = new Label(phone.number());
        number.getStyleClass().add("list-item-title");
        Label sub = new Label(phone.canText() ? phone.type() + "  •  Can text" : phone.type());
        sub.getStyleClass().add("list-item-sub");
        HBox tags = new HBox(4, sub);
        tags.setAlignment(Pos.CENTER_LEFT);
        if (phone.primary()) {
            Label tag = new Label("PRIMARY");
            tag.getStyleClass().add("address-tag");
            tags.getChildren().add(tag);
        }
        VBox text = new VBox(1, number, tags);
        text.getStyleClass().add("list-item");

        Button remove = new Button();
        remove.getStyleClass().add("btn-remove");
        SVGPath cross = new SVGPath();
        cross.setContent("M5 5 L19 19 M19 5 L5 19");
        cross.getStyleClass().add("remove-icon");
        remove.setGraphic(cross);
        remove.setOnAction(e -> removePhone(phone));

        Button edit = new Button("Edit");
        edit.getStyleClass().add("btn-edit-chip");
        edit.setOnAction(e -> openPhoneEditor(phone.id()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(6, text, spacer, remove, edit);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-row");
        return row;
    }

    private void removePhone(PhoneRow phone) {
        if (busy || !confirm("Remove phone number " + phone.number() + "?")) {
            return;
        }
        run(() -> {
            customers.removePhone(customerNumber, phone.id());
            return null;
        }, done -> load("Phone number removed."), "Could not reach the mainframe. The phone number was not removed.");
    }

    @FXML
    private void onAddPhone() {
        if (!busy) {
            openPhoneEditor(null);
        }
    }

    /** @param phoneId the phone number to change, or null to add one */
    private void openPhoneEditor(Long phoneId) {
        String number = customerNumber;
        Runnable backFromContacts = onBack;
        Runnable reopen = () -> Navigator.<ContactsController>show("contacts", c -> c.open(number, backFromContacts));
        Navigator.<EditPhoneController>show("editphone", p -> p.open(number, phoneId, reopen, reopen));
    }

    /**
     * Runs a blocking call off the JavaFX thread. A broken rule is shown as its message;
     * anything else shows {@code unreachable}.
     */
    private <T> void run(Callable<T> work, java.util.function.Consumer<T> then, String unreachable) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            then.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable error = task.getException();
            showNotice(Format.isRuleError(error) ? Inputs.notice(Format.fieldErrors(error, Map.of()))
                    : Format.problem(error, unreachable));
        });
        showNotice("");
        setBusy(true);
        Thread worker = new Thread(task, "contacts");
        worker.setDaemon(true);
        worker.start();
    }

    private boolean confirm(String question) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, question, ButtonType.YES, ButtonType.NO);
        alert.setTitle("Remove");
        alert.setHeaderText(null);
        alert.initOwner(root.getScene().getWindow());
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.YES;
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
        alert.setHeaderText("Contact details");
        alert.setContentText("""
                Esc        Go back
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
