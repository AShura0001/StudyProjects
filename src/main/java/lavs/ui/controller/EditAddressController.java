package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import lavs.business.model.Address;
import lavs.business.service.CustomerService;
import lavs.ui.LavsApp;
import lavs.ui.component.Choices;
import lavs.ui.component.Inputs;

import java.util.Map;

/**
 * Adds an address to a customer, or changes one, through CustomerService.
 * On save the customer's page reopens.
 */
public class EditAddressController extends FormController {

    private static final String DEFAULT_COUNTRY = "New Zealand";

    @FXML private Label titleLabel;
    @FXML private ComboBox<String> typeField;
    @FXML private TextField otherTypeField;
    @FXML private TextField line1Field;
    @FXML private TextField line2Field;
    @FXML private TextField suburbField;
    @FXML private TextField cityField;
    @FXML private TextField postCodeField;
    @FXML private ComboBox<String> countryField;
    @FXML private CheckBox primaryField;
    @FXML private CheckBox mailingField;

    private final CustomerService customers = LavsApp.context().customers();
    private String customerNumber;
    /** The address being changed, or null when adding one. */
    private Long addressId;

    @FXML
    private void initialize() {
        Inputs.choices(typeField, Choices.addressTypes(), t -> t);
        Inputs.choices(countryField, Choices.COUNTRIES, c -> c);
        Inputs.number(postCodeField, 0);
        // The description only matters, and can only be typed, for an "Other" address.
        otherTypeField.disableProperty().bind(typeField.valueProperty().isNotEqualTo("Other"));

        register("type", typeField);
        register("otherType", otherTypeField);
        register("line1", line1Field);
        register("line2", line2Field);
        register("suburb", suburbField);
        register("city", cityField);
        register("postCode", postCodeField);
        register("country", countryField);
        register("isPrimary", primaryField);
        register("isMailing", mailingField);
        setUp(typeField);
    }

    /**
     * Called by the customer's page before this one is displayed.
     *
     * @param addressId the address to change, or null to add a new one
     * @param back what Back does; {@code done} runs after a successful save
     */
    public void open(String customerNumber, Long addressId, Runnable back, Runnable done) {
        this.customerNumber = customerNumber;
        this.addressId = addressId;
        this.onBack = back;
        this.onDone = done;
        if (addressId == null) {
            titleLabel.setText("Add an address");
            Inputs.setValue(typeField, typeField.getItems().get(0));
            Inputs.setValue(countryField, DEFAULT_COUNTRY);
            markClean();
            return;
        }
        titleLabel.setText("Edit address");
        load(() -> customers.findAddress(customerNumber, addressId).orElse(null), address -> {
            if (address == null) {
                showNotice("Could not find that address.");
                return;
            }
            Inputs.setValue(typeField, address.getType());
            Inputs.setValue(otherTypeField, address.getOtherType());
            Inputs.setValue(line1Field, address.getLine1());
            Inputs.setValue(line2Field, address.getLine2());
            Inputs.setValue(suburbField, address.getSuburb());
            Inputs.setValue(cityField, address.getCity());
            Inputs.setValue(postCodeField, address.getPostCode());
            Inputs.setValue(countryField, address.getCountry());
            primaryField.setSelected(Boolean.TRUE.equals(address.getIsPrimary()));
            mailingField.setSelected(Boolean.TRUE.equals(address.getIsMailing()));
        });
    }

    @Override
    protected void submitForm() {
        Address address = new Address();
        address.setCustomerNumber(customerNumber);
        address.setType(Inputs.valueOf(typeField));
        address.setOtherType("Other".equals(address.getType()) ? Inputs.valueOf(otherTypeField) : "");
        address.setLine1(Inputs.valueOf(line1Field));
        address.setLine2(Inputs.valueOf(line2Field));
        address.setSuburb(Inputs.valueOf(suburbField));
        address.setCity(Inputs.valueOf(cityField));
        address.setPostCode(Inputs.valueOf(postCodeField));
        address.setCountry(Inputs.valueOf(countryField));
        address.setIsPrimary(primaryField.isSelected());
        address.setIsMailing(mailingField.isSelected());
        Long id = addressId;
        save(() -> {
            if (id == null) {
                customers.addAddress(address);
            } else {
                address.setId(id);
                customers.updateAddress(address);
            }
            return Map.of();
        });
    }

    @Override
    protected String what() {
        return "address";
    }

    @Override
    protected String screenName() {
        return addressId == null ? "Add address" : "Edit address";
    }
}
