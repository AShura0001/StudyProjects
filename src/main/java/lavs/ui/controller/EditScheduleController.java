package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import lavs.ui.LavsApp;
import lavs.ui.component.Choices;
import lavs.ui.component.Inputs;
import lavs.ui.service.LoanRecords;

/** Edit the terms a loan's repayment schedule is worked out from; on save the loan's page reopens. */
public class EditScheduleController extends FormController {

    @FXML private TextField paymentAmountField;
    @FXML private ComboBox<String> paymentFrequencyField;
    @FXML private DatePicker renewalDateField;

    private final LoanRecords loanService = LavsApp.context().loans();
    private String loanNumber;

    @FXML
    private void initialize() {
        Inputs.number(paymentAmountField, 2);
        Inputs.choices(paymentFrequencyField, Choices.PAYMENT_FREQUENCIES, Choices::label);
        Inputs.date(renewalDateField);
        register("paymentAmount", paymentAmountField);
        register("paymentFrequency", paymentFrequencyField);
        register("renewalDate", renewalDateField);
        setUp(paymentAmountField);
    }

    /**
     * Called by the loan's page before this one is displayed.
     *
     * @param back what Back does; {@code done} runs after a successful save
     */
    public void open(String loanNumber, Runnable back, Runnable done) {
        this.loanNumber = loanNumber;
        this.onBack = back;
        this.onDone = done;
        load(() -> loanService.scheduleTerms(loanNumber), terms -> {
            if (terms == null) {
                showNotice("Could not find that loan.");
                return;
            }
            Inputs.setValue(paymentAmountField, terms.paymentAmount());
            Inputs.setValue(paymentFrequencyField, terms.paymentFrequency());
            Inputs.setValue(renewalDateField, terms.renewalDate());
        });
    }

    @Override
    protected void submitForm() {
        LoanRecords.ScheduleTerms edited = new LoanRecords.ScheduleTerms(Inputs.valueOf(paymentAmountField),
                Inputs.valueOf(paymentFrequencyField), Inputs.valueOf(renewalDateField));
        save(() -> loanService.saveScheduleTerms(loanNumber, edited));
    }

    @Override
    protected String what() {
        return "repayment schedule";
    }

    @Override
    protected String screenName() {
        return "Edit repayment schedule";
    }
}
