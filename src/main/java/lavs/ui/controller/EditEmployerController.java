package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import lavs.ui.LavsApp;
import lavs.ui.component.Inputs;
import lavs.business.model.Employer;
import lavs.business.service.CustomerService;

/** Edit one of a customer's employers; on save the customer's page reopens. */
public class EditEmployerController extends FormController {

    @FXML private TextField nameField;
    @FXML private TextField positionField;
    @FXML private TextField salaryField;

    private final CustomerService customerService = LavsApp.context().customers();
    private String customerNumber;
    private String employerId;

    @FXML
    private void initialize() {
        Inputs.number(salaryField, 2);
        register("name", nameField);
        register("position", positionField);
        register("salary", salaryField);
        setUp(nameField);
    }

    /**
     * Called by the customer's page before this one is displayed.
     *
     * @param back what Back does; {@code done} runs after a successful save
     */
    public void open(String customerNumber, String employerId, Runnable back, Runnable done) {
        this.customerNumber = customerNumber;
        this.employerId = employerId;
        this.onBack = back;
        this.onDone = done;
        load(() -> customerService.findEmployer(customerNumber, idOf(employerId)).orElse(null), employer -> {
            if (employer == null) {
                showNotice("Could not find that employer.");
                return;
            }
            nameField.setText(employer.get_name() == null ? "" : employer.get_name());
            positionField.setText(employer.get_position() == null ? "" : employer.get_position());
            salaryField.setText(String.format(java.util.Locale.US, "%.2f", employer.get_salary()));
        });
    }

    @Override
    protected void submitForm() {
        String name = Inputs.valueOf(nameField);
        String position = Inputs.valueOf(positionField);
        String salaryText = Inputs.valueOf(salaryField).replace("$", "").replace(",", "");
        save(() -> {
            double salary;
            try {
                salary = salaryText.isEmpty() ? 0 : Double.parseDouble(salaryText);
            } catch (NumberFormatException e) {
                return java.util.Map.of("salary", "Salary must be a number such as 85000.00.");
            }
            Employer employer = customerService.findEmployer(customerNumber, idOf(employerId)).orElse(null);
            if (employer == null) {
                return java.util.Map.of("form", "That employer no longer exists.");
            }
            employer.set_customerNumber(customerNumber);
            employer.set_name(name);
            employer.set_position(position);
            employer.set_salary(salary);
            customerService.updateEmployer(employer);
            return java.util.Map.of();
        });
    }

    private static long idOf(String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    protected String what() {
        return "employer";
    }

    @Override
    protected String screenName() {
        return "Edit employer";
    }
}
