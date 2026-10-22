package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import lavs.business.model.Email;
import lavs.business.service.CustomerService;
import lavs.ui.LavsApp;
import lavs.ui.component.Choices;
import lavs.ui.component.Inputs;

import java.util.Map;

/**
 * Adds an email to a customer, or changes one, through CustomerService.
 * On save the customer's contact details reopen.
 */
public class EditEmailController extends FormController {

    @FXML private Label titleLabel;
    @FXML private ComboBox<String> typeField;
    @FXML private TextField otherTypeField;
    @FXML private TextField addressField;
    @FXML private CheckBox primaryField;

    private final CustomerService customers = LavsApp.context().customers();
    private String customerNumber;
    /** The email being changed, or null when adding one. */
    private Long emailId;

    @FXML
    private void initialize() {
        Inputs.choices(typeField, Choices.emailTypes(), t -> t);
        // The description only matters, and can only be typed, for an "Other" email.
        otherTypeField.disableProperty().bind(typeField.valueProperty().isNotEqualTo("Other"));

        register("type", typeField);
        register("otherType", otherTypeField);
        register("address", addressField);
        register("isPrimary", primaryField);
        setUp(typeField);
    }

    /**
     * Called by the contact details screen before this one is displayed.
     *
     * @param emailId the email to change, or null to add a new one
     * @param back what Back does; {@code done} runs after a successful save
     */
    public void open(String customerNumber, Long emailId, Runnable back, Runnable done) {
        this.customerNumber = customerNumber;
        this.emailId = emailId;
        this.onBack = back;
        this.onDone = done;
        if (emailId == null) {
            titleLabel.setText("Add an email");
            Inputs.setValue(typeField, typeField.getItems().get(0));
            markClean();
            return;
        }
        titleLabel.setText("Edit email");
        load(() -> customers.findEmail(customerNumber, emailId).orElse(null), email -> {
            if (email == null) {
                showNotice("Could not find that email.");
                return;
            }
            Inputs.setValue(typeField, email.getType());
            Inputs.setValue(otherTypeField, email.getOtherType());
            Inputs.setValue(addressField, email.getAddress());
            primaryField.setSelected(email.isPrimary());
        });
    }

    @Override
    protected void submitForm() {
        String type = Inputs.valueOf(typeField);
        Email email = new Email(Inputs.valueOf(addressField), null, customerNumber,
                emailId == null ? 0 : emailId, primaryField.isSelected(),
                "Other".equals(type) ? Inputs.valueOf(otherTypeField) : "", type);
        boolean adding = emailId == null;
        save(() -> {
            if (adding) {
                customers.addEmail(email);
            } else {
                customers.updateEmail(email);
            }
            return Map.of();
        });
    }

    @Override
    protected String what() {
        return "email";
    }

    @Override
    protected String screenName() {
        return emailId == null ? "Add email" : "Edit email";
    }
}
