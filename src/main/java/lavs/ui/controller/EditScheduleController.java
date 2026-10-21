package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import lavs.business.model.LoanDetails;
import lavs.business.model.LoanRecord;
import lavs.business.service.LoanAccountService;
import lavs.ui.LavsApp;
import lavs.ui.component.Choices;
import lavs.ui.component.Format;
import lavs.ui.component.Inputs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Edit the terms a loan's repayment schedule is worked out from; on save the loan's page reopens. */
public class EditScheduleController extends FormController {

    @FXML private TextField paymentAmountField;
    @FXML private ComboBox<String> paymentFrequencyField;
    @FXML private DatePicker renewalDateField;

    private final LoanAccountService loanAccounts = LavsApp.context().loanAccounts();
    private String loanNumber;

    @FXML
    private void initialize() {
        Inputs.number(paymentAmountField, 2);
        Inputs.choices(paymentFrequencyField, Choices.paymentFrequencies(), Choices::label);
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
        load(() -> loanAccounts.find(loanNumber).orElse(null), loan -> {
            if (loan == null) {
                showNotice("Could not find that loan.");
                return;
            }
            Inputs.setValue(paymentAmountField, Format.plain(loan.paymentAmount()));
            Inputs.setValue(paymentFrequencyField, loan.paymentFrequency());
            Inputs.setValue(renewalDateField, Format.date(loan.renewalDate()));
        });
    }

    @Override
    protected void submitForm() {
        String amountText = Inputs.valueOf(paymentAmountField).replace("$", "").replace(",", "");
        String frequency = Inputs.valueOf(paymentFrequencyField);
        String renewalText = Inputs.valueOf(renewalDateField);
        save(() -> {
            Map<String, String> missing = new LinkedHashMap<>();
            if (amountText.isEmpty()) {
                missing.put("paymentAmount", "Payment amount is required.");
            }
            if (frequency.isEmpty()) {
                missing.put("paymentFrequency", "Choose how often payments are made.");
            }
            if (renewalText.isEmpty()) {
                missing.put("renewalDate", "Renewal date is required.");
            }
            if (!missing.isEmpty()) {
                return missing;
            }
            LoanRecord loan = loanAccounts.find(loanNumber).orElse(null);
            LoanDetails current = loan == null ? null : LoanAccountService.termsOf(loan).orElse(null);
            if (current == null) {
                return Map.of("form", "The mainframe is missing some of this loan's terms, so they cannot be changed here.");
            }
            loanAccounts.updateTerms(loanNumber, new LoanDetails(current.principal(), current.rateType(),
                    current.rateAmount(), current.rateFrequency(), new BigDecimal(amountText), frequency,
                    current.startDate(), LocalDate.parse(renewalText)));
            return Map.of();
        });
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
