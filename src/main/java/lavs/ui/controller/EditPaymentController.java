package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import lavs.ui.component.Inputs;

import java.util.Map;

/**
 * One repayment of a loan's schedule. The mainframe works every repayment out from
 * the loan's terms and cannot change one on its own, so Save explains that and points
 * to Edit schedule; there is no business service behind this form.
 */
public class EditPaymentController extends FormController {

    static final String NOT_POSSIBLE = "The mainframe works each repayment out from the loan's terms, "
            + "so one repayment cannot be changed on its own. Use Edit schedule to change the terms.";

    @FXML private Label titleLabel;
    @FXML private DatePicker dateField;
    @FXML private TextField amountField;

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
        this.onBack = back;
        this.onDone = done;
        titleLabel.setText("Payment " + paymentNumber + " of " + loanNumber);
        Inputs.setValue(dateField, date);
        amountField.setText(amount.replace("$", "").replace(",", ""));
        markClean();
    }

    @Override
    protected void submitForm() {
        save(() -> Map.of("form", NOT_POSSIBLE));
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
