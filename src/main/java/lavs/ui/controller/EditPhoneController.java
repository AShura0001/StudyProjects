package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import lavs.business.model.PhoneNumber;
import lavs.business.service.CustomerService;
import lavs.ui.LavsApp;
import lavs.ui.component.Choices;
import lavs.ui.component.Inputs;

import java.util.Map;

/**
 * Adds a phone number to a customer, or changes one, through CustomerService.
 * On save the customer's contact details reopen.
 */
public class EditPhoneController extends FormController {

    private static final String DEFAULT_CODE = "+64";

    @FXML private Label titleLabel;
    @FXML private ComboBox<String> typeField;
    @FXML private TextField otherTypeField;
    @FXML private ComboBox<String> countryCodeField;
    @FXML private TextField numberField;
    @FXML private CheckBox canTextField;
    @FXML private CheckBox primaryField;

    private final CustomerService customers = LavsApp.context().customers();
    private String customerNumber;
    /** The phone number being changed, or null when adding one. */
    private Long phoneId;

    @FXML
    private void initialize() {
        Inputs.choices(typeField, Choices.phoneTypes(), t -> t);
        Inputs.shortChoices(countryCodeField, Choices.COUNTRY_CODES, Choices::codeLabel);
        // The description only matters, and can only be typed, for an "Other" phone number.
        otherTypeField.disableProperty().bind(typeField.valueProperty().isNotEqualTo("Other"));

        register("type", typeField);
        register("otherType", otherTypeField);
        register("countryCode", countryCodeField);
        register("number", numberField);
        register("canTxt", canTextField);
        register("isPrimary", primaryField);
        setUp(typeField);
    }

    /**
     * Called by the contact details screen before this one is displayed.
     *
     * @param phoneId the phone number to change, or null to add a new one
     * @param back what Back does; {@code done} runs after a successful save
     */
    public void open(String customerNumber, Long phoneId, Runnable back, Runnable done) {
        this.customerNumber = customerNumber;
        this.phoneId = phoneId;
        this.onBack = back;
        this.onDone = done;
        if (phoneId == null) {
            titleLabel.setText("Add a phone number");
            Inputs.setValue(typeField, typeField.getItems().get(0));
            Inputs.setValue(countryCodeField, DEFAULT_CODE);
            markClean();
            return;
        }
        titleLabel.setText("Edit phone number");
        load(() -> customers.findPhone(customerNumber, phoneId).orElse(null), phone -> {
            if (phone == null) {
                showNotice("Could not find that phone number.");
                return;
            }
            Inputs.setValue(typeField, phone.get_type());
            Inputs.setValue(otherTypeField, phone.get_otherType());
            Inputs.setValue(countryCodeField, phone.get_countryCode());
            Inputs.setValue(numberField, phone.get_number());
            canTextField.setSelected(phone.is_canTxt());
            primaryField.setSelected(phone.is_isPrimary());
        });
    }

    @Override
    protected void submitForm() {
        String type = Inputs.valueOf(typeField);
        PhoneNumber phone = new PhoneNumber(canTextField.isSelected(), Inputs.valueOf(countryCodeField), null,
                customerNumber, phoneId == null ? 0 : phoneId, primaryField.isSelected(),
                "Other".equals(type) ? Inputs.valueOf(otherTypeField) : "", type);
        phone.set_number(Inputs.valueOf(numberField));
        boolean adding = phoneId == null;
        save(() -> {
            if (adding) {
                customers.addPhone(phone);
            } else {
                customers.updatePhone(phone);
            }
            return Map.of();
        });
    }

    @Override
    protected String what() {
        return "phone number";
    }

    @Override
    protected String screenName() {
        return phoneId == null ? "Add phone number" : "Edit phone number";
    }
}
