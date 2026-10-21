package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import lavs.ui.LavsApp;
import lavs.ui.component.Choices;
import lavs.ui.component.Inputs;
import lavs.business.model.Employer;
import lavs.business.service.CustomerService;

import java.util.Locale;
import java.util.Map;

/**
 * Adds an employer to a customer, or changes one, through CustomerService.
 * On save the customer's page reopens.
 */
public class EditEmployerController extends FormController {

    @FXML private Label titleLabel;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> typeField;
    @FXML private TextField positionField;
    @FXML private TextField salaryField;

    private final CustomerService customerService = LavsApp.context().customers();
    private String customerNumber;
    /** The employer being changed, or null when adding one. */
    private Long employerId;

    @FXML
    private void initialize() {
        Inputs.number(salaryField, 2);
        Inputs.choices(typeField, Choices.EMPLOYER_TYPES, t -> t);
        register("name", nameField);
        register("type", typeField);
        register("position", positionField);
        register("salary", salaryField);
        setUp(nameField);
    }

    /**
     * Called by the customer's page before this one is displayed.
     *
     * @param employerId the employer to change, or null to add a new one
     * @param back what Back does; {@code done} runs after a successful save
     */
    public void open(String customerNumber, String employerId, Runnable back, Runnable done) {
        this.customerNumber = customerNumber;
        this.employerId = employerId == null ? null : idOf(employerId);
        this.onBack = back;
        this.onDone = done;
        if (this.employerId == null) {
            titleLabel.setText("Add an employer");
            // A customer's first employer is their main one.
            load(() -> customerService.listEmployers(customerNumber).isEmpty(), first -> {
                Inputs.setValue(typeField, first ? "Primary" : "Secondary");
            });
            return;
        }
        titleLabel.setText("Edit employer");
        long id = this.employerId;
        load(() -> customerService.findEmployer(customerNumber, id).orElse(null), employer -> {
            if (employer == null) {
                showNotice("Could not find that employer.");
                return;
            }
            nameField.setText(employer.get_name() == null ? "" : employer.get_name());
            Inputs.setValue(typeField, employer.get_type());
            positionField.setText(employer.get_position() == null ? "" : employer.get_position());
            salaryField.setText(String.format(Locale.US, "%.2f", employer.get_salary()));
        });
    }

    @Override
    protected void submitForm() {
        String name = Inputs.valueOf(nameField);
        String type = Inputs.valueOf(typeField);
        String position = Inputs.valueOf(positionField);
        String salaryText = Inputs.valueOf(salaryField).replace("$", "").replace(",", "");
        Long id = employerId;
        save(() -> {
            double salary;
            try {
                salary = salaryText.isEmpty() ? 0 : Double.parseDouble(salaryText);
            } catch (NumberFormatException e) {
                return Map.of("salary", "Salary must be a number such as 85000.00.");
            }
            Employer employer;
            if (id == null) {
                employer = new Employer();
            } else {
                employer = customerService.findEmployer(customerNumber, id).orElse(null);
                if (employer == null) {
                    return Map.of("form", "That employer no longer exists.");
                }
            }
            employer.set_customerNumber(customerNumber);
            employer.set_name(name);
            employer.set_type(type);
            employer.set_position(position);
            employer.set_salary(salary);
            if (id == null) {
                customerService.addEmployer(employer);
            } else {
                customerService.updateEmployer(employer);
            }
            return Map.of();
        });
    }

    private static Long idOf(String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    @Override
    protected String what() {
        return "employer";
    }

    @Override
    protected String screenName() {
        return employerId == null ? "Add employer" : "Edit employer";
    }
}
