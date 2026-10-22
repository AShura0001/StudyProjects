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

import lavs.business.model.Email;
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
 * Every phone number and email of one customer, read and changed through CustomerService.
 * Each is added and changed on its own form; a removal is confirmed and sent at once.
 */
public class ContactsController {

    /** One phone number or email: what is shown on its two lines, and whether it is the primary one. */
    private record ContactRow(long id, String title, String sub, boolean primary) {
    }

    private record Contacts(List<ContactRow> phones, List<ContactRow> emails) {
    }

    private static final Comparator<ContactRow> PRIMARY_FIRST = Comparator.comparing(c -> !c.primary());

    @FXML private BorderPane root;
    @FXML private Node content;
    @FXML private ProgressIndicator progress;
    @FXML private Label customerLabel;
    @FXML private VBox phonesBox;
    @FXML private VBox emailsBox;
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
        run(() -> new Contacts(
                customers.listPhones(customerNumber).stream().map(ContactsController::phoneRow)
                        .sorted(PRIMARY_FIRST).toList(),
                customers.listEmails(customerNumber).stream().map(ContactsController::emailRow)
                        .sorted(PRIMARY_FIRST).toList()), contacts -> {
            render(phonesBox, contacts.phones(), "No phone numbers yet.", this::removePhone, this::openPhoneEditor);
            render(emailsBox, contacts.emails(), "No emails yet.", this::removeEmail, this::openEmailEditor);
            showNotice(noticeWhenDone);
        }, "Could not reach the mainframe. Try again.");
    }

    private static ContactRow phoneRow(PhoneNumber phone) {
        String type = typeName(phone.get_type(), phone.get_otherType());
        String number = (Format.text(phone.get_countryCode()) + " " + Format.text(phone.get_number())).trim();
        return new ContactRow(phone.get_id(), number.isEmpty() ? "(no number)" : number,
                phone.is_canTxt() ? type + "  •  Can text" : type, phone.is_isPrimary());
    }

    private static ContactRow emailRow(Email email) {
        String address = Format.text(email.getAddress());
        return new ContactRow(email.getId(), address.isEmpty() ? "(no address)" : address,
                typeName(email.getType(), email.getOtherType()), email.isPrimary());
    }

    /** "Other" reads as its description when there is one. */
    private static String typeName(String type, String otherType) {
        String name = Format.text(type);
        return "Other".equalsIgnoreCase(name) && !Format.text(otherType).isEmpty() ? Format.text(otherType) : name;
    }

    private void render(VBox box, List<ContactRow> rows, String empty,
                        java.util.function.Consumer<ContactRow> remove, java.util.function.Consumer<Long> edit) {
        box.getChildren().clear();
        if (rows.isEmpty()) {
            Label none = new Label(empty);
            none.getStyleClass().add("list-item-sub");
            box.getChildren().add(none);
        }
        for (ContactRow row : rows) {
            box.getChildren().add(contactItem(row, remove, edit));
        }
    }

    /** The number or address, then its type with a Primary tag, then remove and Edit. */
    private Node contactItem(ContactRow contact, java.util.function.Consumer<ContactRow> onRemove,
                             java.util.function.Consumer<Long> onEdit) {
        Label title = new Label(contact.title());
        title.getStyleClass().add("list-item-title");
        Label sub = new Label(contact.sub());
        sub.getStyleClass().add("list-item-sub");
        HBox tags = new HBox(4, sub);
        tags.setAlignment(Pos.CENTER_LEFT);
        if (contact.primary()) {
            Label tag = new Label("PRIMARY");
            tag.getStyleClass().add("address-tag");
            tags.getChildren().add(tag);
        }
        VBox text = new VBox(1, title, tags);
        text.getStyleClass().add("list-item");

        Button remove = new Button();
        remove.getStyleClass().add("btn-remove");
        SVGPath cross = new SVGPath();
        cross.setContent("M5 5 L19 19 M19 5 L5 19");
        cross.getStyleClass().add("remove-icon");
        remove.setGraphic(cross);
        remove.setOnAction(e -> onRemove.accept(contact));

        Button edit = new Button("Edit");
        edit.getStyleClass().add("btn-edit-chip");
        edit.setOnAction(e -> onEdit.accept(contact.id()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(6, text, spacer, remove, edit);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-row");
        return row;
    }

    private void removePhone(ContactRow phone) {
        if (busy || !confirm("Remove phone number " + phone.title() + "?")) {
            return;
        }
        run(() -> {
            customers.removePhone(customerNumber, phone.id());
            return null;
        }, done -> load("Phone number removed."), "Could not reach the mainframe. The phone number was not removed.");
    }

    private void removeEmail(ContactRow email) {
        if (busy || !confirm("Remove email " + email.title() + "?")) {
            return;
        }
        run(() -> {
            customers.removeEmail(customerNumber, email.id());
            return null;
        }, done -> load("Email removed."), "Could not reach the mainframe. The email was not removed.");
    }

    @FXML
    private void onAddEmail() {
        if (!busy) {
            openEmailEditor(null);
        }
    }

    /** @param emailId the email to change, or null to add one */
    private void openEmailEditor(Long emailId) {
        String number = customerNumber;
        Runnable backFromContacts = onBack;
        Runnable reopen = () -> Navigator.<ContactsController>show("contacts", c -> c.open(number, backFromContacts));
        Navigator.<EditEmailController>show("editemail", e -> e.open(number, emailId, reopen, reopen));
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
