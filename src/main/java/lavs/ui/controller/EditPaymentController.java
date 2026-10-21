package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import lavs.ui.LavsApp;
import lavs.ui.component.Inputs;
import lavs.ui.service.LoanRecords;

/** Edit one repayment of a loan; on save the loan's page reopens. */
public class EditPaymentController extends FormController {

    @FXML private Label titleLabel;
    @FXML private DatePicker dateField;
    @FXML private TextField amountField;

    private final LoanRecords loanService = LavsApp.context().loans();
    private String loanNumber;
    private int paymentNumber;

    @FXML
    private void initialize() {
        Inputs.date(dateField);
        Inputs.number(amountField, 2);
        register("date", dateField);
        register("amount", amountField);
        setUp(dateField);
    }

    /**
     * Called by the loan's page before this one is displayed.
     *
     * @param amount the repayment's total as shown on the loan page, e.g. "$1,234.56"
     * @param back what Back does; {@code done} runs after a successful save
     */
    public void open(String loanNumber, int paymentNumber, String date, String amount, Runnable back, Runnable done) {
        this.loanNumber = loanNumber;
        this.paymentNumber = paymentNumber;
        this.onBack = back;
        this.onDone = done;
        titleLabel.setText("Edit payment " + paymentNumber);
        Inputs.setValue(dateField, date);
        amountField.setText(amount.replace("$", "").replace(",", ""));
        markClean();
    }

    @Override
    protected void submitForm() {
        LoanRecords.PaymentDetails edited = new LoanRecords.PaymentDetails(paymentNumber,
                Inputs.valueOf(dateField), Inputs.valueOf(amountField));
        save(() -> loanService.savePayment(loanNumber, edited));
    }

    @Override
    protected String what() {
        return "payment";
    }

    @Override
    protected String screenName() {
        return "Edit payment";
    }
}
