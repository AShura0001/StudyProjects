package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import lavs.ui.LavsApp;
import lavs.ui.component.Inputs;
import lavs.ui.service.CustomerDirectory;

/** Edit one of a customer's employers; on save the customer's page reopens. */
public class EditEmployerController extends FormController {

    @FXML private TextField nameField;
    @FXML private TextField positionField;
    @FXML private TextField salaryField;

    private final CustomerDirectory customerService = LavsApp.context().customers();
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
        load(() -> customerService.employer(customerNumber, employerId), employer -> {
            if (employer == null) {
                showNotice("Could not find that employer.");
                return;
            }
            nameField.setText(employer.name());
            positionField.setText(employer.position());
            salaryField.setText(employer.salary());
        });
    }

    @Override
    protected void submitForm() {
        CustomerDirectory.EmployerDetails edited = new CustomerDirectory.EmployerDetails(employerId,
                nameField.getText().trim(), positionField.getText().trim(), salaryField.getText().trim());
        save(() -> customerService.saveEmployer(customerNumber, edited));
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
