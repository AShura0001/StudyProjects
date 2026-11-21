package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import lavs.business.model.RelationshipDetails;
import lavs.business.model.RelationshipType;
import lavs.business.service.CustomerRelationshipService;
import lavs.ui.LavsApp;
import lavs.ui.component.Inputs;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds a relationship from this customer to another, or changes one. On save the
 * customer's relationships reopen. The rules (not the customer themselves, an "Other"
 * relationship needs a description, both customers must exist) are checked by
 * {@link CustomerRelationshipService}; problems come back against the fields.
 */
public class EditRelationshipController extends FormController {

    private static final List<String> TYPES =
            Arrays.stream(RelationshipType.values()).map(RelationshipType::mainframeName).toList();

    @FXML private Label titleLabel;
    @FXML private ComboBox<String> typeField;
    @FXML private TextField descriptionField;
    @FXML private TextField relatedCustomerField;

    private final CustomerRelationshipService relationships = LavsApp.context().relationships();
    private String customerNumber;
    /** The relationship being changed, or null when adding one. */
    private Long relationshipId;

    @FXML
    private void initialize() {
        Inputs.choices(typeField, TYPES, name -> RelationshipType.fromMainframeName(name)
                .map(RelationshipType::label).orElse(name));
        // The description only matters, and can only be typed, for an "Other" relationship.
        descriptionField.disableProperty().bind(
                typeField.valueProperty().isNotEqualTo(RelationshipType.OTHER.mainframeName()));

        register("type", typeField);
        register(CustomerRelationshipService.DESCRIPTION_FIELD, descriptionField);
        register(CustomerRelationshipService.RELATED_CUSTOMER_FIELD, relatedCustomerField);
        setUp(typeField);
    }

    /**
     * Called by the customer's relationships before this one is displayed.
     *
     * @param relationshipId the relationship to change, or null to add a new one
     * @param back what Back does; {@code done} runs after a successful save
     */
    public void open(String customerNumber, Long relationshipId, Runnable back, Runnable done) {
        this.customerNumber = customerNumber;
        this.relationshipId = relationshipId;
        this.onBack = back;
        this.onDone = done;
        if (relationshipId == null) {
            titleLabel.setText("Add a relationship");
            markClean();
            return;
        }
        titleLabel.setText("Change relationship");
        load(() -> relationships.find(customerNumber, relationshipId).orElse(null), relationship -> {
            if (relationship == null) {
                showNotice("Could not find that relationship.");
                return;
            }
            RelationshipType.fromMainframeName(relationship.get_type())
                    .ifPresent(type -> typeField.setValue(type.mainframeName()));
            descriptionField.setText(relationship.get_otherType() == null ? "" : relationship.get_otherType());
            relatedCustomerField.setText(relationship.get_secondaryCustomerNumber() == null
                    ? "" : relationship.get_secondaryCustomerNumber());
        });
    }

    @Override
    protected void submitForm() {
        String typeName = typeField.getValue();
        String description = descriptionField.getText();
        String related = Inputs.valueOf(relatedCustomerField);
        String customer = customerNumber;
        Long id = relationshipId;
        save(() -> {
            Map<String, String> missing = new LinkedHashMap<>();
            if (typeName == null) {
                missing.put("type", "Choose the kind of relationship.");
            }
            if (related.isEmpty()) {
                missing.put(CustomerRelationshipService.RELATED_CUSTOMER_FIELD, "Enter the other customer's number.");
            }
            if (!missing.isEmpty()) {
                return missing;
            }
            RelationshipType type = RelationshipType.fromMainframeName(typeName).orElse(RelationshipType.OTHER);
            RelationshipDetails details = new RelationshipDetails(type, description, related);
            if (id == null) {
                relationships.add(customer, details);
            } else {
                relationships.update(customer, id, details);
            }
            return Map.of();
        });
    }

    @Override
    protected String what() {
        return "relationship";
    }

    @Override
    protected String screenName() {
        return relationshipId == null ? "Add relationship" : "Change relationship";
    }
}
