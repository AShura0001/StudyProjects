package lavs.ui.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;

import lavs.ui.LavsApp;
import lavs.ui.component.Choices;
import lavs.ui.component.Inputs;
import lavs.ui.Navigator;
import lavs.business.model.CustomerReference;
import lavs.business.model.LoanDetails;
import lavs.business.service.LoanService;
import lavs.ui.component.Format;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Add a loan to a customer; on success the new loan's details open. */
public class AddLoanController {

    @FXML private BorderPane root;
    @FXML private Node content;
    @FXML private ProgressIndicator progress;
    @FXML private TextField principalField;
    @FXML private DatePicker startDateField;
    @FXML private DatePicker renewalDateField;
    @FXML private ComboBox<String> rateTypeField;
    @FXML private TextField rateAmountField;
    @FXML private ComboBox<String> rateFrequencyField;
    @FXML private TextField paymentAmountField;
    @FXML private ComboBox<String> paymentFrequencyField;
    @FXML private Label noticeLabel;

    private final LoanService loanService = LavsApp.context().loans();
    private final Map<String, Control> byKey = new LinkedHashMap<>();
    private String customerNumber;
    /** The refused or waiting loan draft being fixed, or null when adding a new loan. */
    private String fixingDraftId;
    @FXML private Label titleLabel;
    @FXML private javafx.scene.control.Button submitButton;
    private Runnable onBack = () -> Navigator.show("customersearch");
    private boolean busy;

    @FXML
    private void initialize() {
        Inputs.number(principalField, 2);
        Inputs.date(startDateField);
        Inputs.date(renewalDateField);
        Inputs.choices(rateTypeField, Choices.RATE_TYPES, Choices::label);
        Inputs.number(rateAmountField, 3);
        Inputs.choices(rateFrequencyField, Choices.RATE_FREQUENCIES, Choices::label);
        Inputs.number(paymentAmountField, 2);
        Inputs.choices(paymentFrequencyField, Choices.PAYMENT_FREQUENCIES, Choices::label);

        byKey.put("principal", principalField);
        byKey.put("startDate", startDateField);
        byKey.put("renewalDate", renewalDateField);
        byKey.put("rateType", rateTypeField);
        byKey.put("rateAmount", rateAmountField);
        byKey.put("rateFrequency", rateFrequencyField);
        byKey.put("paymentAmount", paymentAmountField);
        byKey.put("paymentFrequency", paymentFrequencyField);

        setShown(noticeLabel, false);
        setShown(progress, false);
        for (Control field : byKey.values()) {
            Inputs.onEdit(field, () -> field.getStyleClass().remove("field-error"));
        }
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerAccelerators(scene);
                Platform.runLater(principalField::requestFocus);
            }
        });
    }

    /** Called by the customer's page before this one is displayed. */
    public void open(String customerNumber, Runnable back) {
        this.customerNumber = customerNumber;
        this.onBack = back;
    }

    /**
     * Opens a saved loan that was refused (or is still waiting), filled in, so it can be
     * corrected and sent again. Called from the saved changes window.
     */
    public void openToFix(String draftId, Runnable back) {
        this.fixingDraftId = draftId;
        this.onBack = back;
        titleLabel.setText("Fix a saved loan");
        submitButton.setText("resend");
        Optional<lavs.business.model.Draft> draft = LavsApp.context().drafts().findById(draftId);
        if (draft.isEmpty()) {
            showNotice("That saved loan no longer exists.");
            return;
        }
        lavs.business.model.LoanPayloads.LoanContent loan =
                lavs.business.model.LoanPayloads.readLoan(draft.get().getPayload());
        if (loan.customer() instanceof CustomerReference.ExistingCustomer existing) {
            customerNumber = existing.customerNumber();
        }
        LoanDetails d = loan.details();
        Inputs.setValue(principalField, Format.plain(d.principal()));
        Inputs.setValue(startDateField, d.startDate().toString());
        Inputs.setValue(renewalDateField, d.renewalDate().toString());
        Inputs.setValue(rateTypeField, d.rateType());
        Inputs.setValue(rateAmountField, d.rateAmount().stripTrailingZeros().toPlainString());
        Inputs.setValue(rateFrequencyField, d.rateFrequency());
        Inputs.setValue(paymentAmountField, Format.plain(d.paymentAmount()));
        Inputs.setValue(paymentFrequencyField, d.paymentFrequency());
        if (!draft.get().getErrors().isEmpty()) {
            showNotice("The mainframe refused this loan: " + String.join("  ", draft.get().getErrors().values()));
        }
    }

    private boolean isDirty() {
        return byKey.values().stream().anyMatch(f -> !Inputs.valueOf(f).isBlank());
    }

    @FXML
    private void onSubmit() {
        if (busy) {
            return;
        }
        Map<String, String> badDates = Inputs.badDates(byKey);
        if (!badDates.isEmpty()) {
            showFieldErrors(badDates);
            return;
        }
        Map<String, String> missing = new LinkedHashMap<>();
        requireValue(missing, "principal", "Loan amount is required.");
        requireValue(missing, "startDate", "Start date is required.");
        requireValue(missing, "renewalDate", "Renewal date is required.");
        requireValue(missing, "rateType", "Choose a rate type.");
        requireValue(missing, "rateAmount", "Interest rate is required.");
        requireValue(missing, "rateFrequency", "Choose a rate frequency.");
        requireValue(missing, "paymentAmount", "Payment amount is required.");
        requireValue(missing, "paymentFrequency", "Choose a payment frequency.");
        if (!missing.isEmpty()) {
            showFieldErrors(missing);
            return;
        }
        LoanDetails details = new LoanDetails(amount(principalField), Inputs.valueOf(rateTypeField),
                amount(rateAmountField), Inputs.valueOf(rateFrequencyField),
                amount(paymentAmountField), Inputs.valueOf(paymentFrequencyField),
                java.time.LocalDate.parse(Inputs.valueOf(startDateField)),
                java.time.LocalDate.parse(Inputs.valueOf(renewalDateField)));
        String fixing = fixingDraftId;
        CustomerReference customer = fixing == null ? CustomerReference.existing(customerNumber) : null;

        Task<lavs.business.model.Draft> task = new Task<>() {
            @Override
            protected lavs.business.model.Draft call() {
                return fixing == null
                        ? loanService.createLoan(customer, details)
                        : loanService.reviseLoan(fixing, details);
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false);
            Alert saved = new Alert(Alert.AlertType.INFORMATION,
                    "The loan is saved and will be sent to the mainframe in the background. "
                            + "The sync dots at the top show when it has been sent.", ButtonType.OK);
            saved.setTitle("Loan saved");
            saved.setHeaderText(null);
            saved.initOwner(root.getScene().getWindow());
            saved.showAndWait();
            onBack.run();
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable error = task.getException();
            if (Format.isRuleError(error)) {
                showFieldErrors(Format.fieldErrors(error, Map.of("principal", "principal",
                        "rateAmount", "rateAmount", "paymentAmount", "paymentAmount", "renewalDate", "renewalDate")));
            } else {
                showNotice(Format.problem(error, "The loan could not be saved. Your details are still on screen."));
            }
        });

        showNotice("");
        setBusy(true);
        Thread worker = new Thread(task, "loan-create");
        worker.setDaemon(true);
        worker.start();
    }

    private void requireValue(Map<String, String> missing, String key, String message) {
        if (Inputs.valueOf(byKey.get(key)).isEmpty()) {
            missing.put(key, message);
        }
    }

    private static java.math.BigDecimal amount(TextField field) {
        return new java.math.BigDecimal(Inputs.valueOf(field).replace("$", "").replace(",", ""));
    }

    private void showFieldErrors(Map<String, String> errors) {
        Control first = null;
        for (String key : errors.keySet()) {
            Control field = byKey.get(key);
            if (field != null) {
                field.getStyleClass().add("field-error");
                if (first == null) {
                    first = field;
                }
            }
        }
        showNotice(Inputs.notice(errors));
        if (first != null) {
            first.requestFocus();
        }
    }

    @FXML
    private void onBack() {
        if (!busy && confirmDiscard()) {
            onBack.run();
        }
    }

    @FXML
    private void onLogout() {
        if (!busy && confirmDiscard()) {
            LavsApp.context().logout();
            Navigator.show("loggedout");
        }
    }

    private boolean confirmDiscard() {
        if (!isDirty()) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "You have not added this loan yet. Discard what you typed?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Unsaved details");
        alert.setHeaderText(null);
        alert.initOwner(root.getScene().getWindow());
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.YES;
    }

    /** Each accelerator is a second route to a control that is already on screen. */
    private void registerAccelerators(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::onBack);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::onSubmit);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN), this::onLogout);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), this::showShortcuts);
    }

    private void setBusy(boolean value) {
        busy = value;
        setShown(progress, value);
        content.setOpacity(value ? 0.35 : 1.0);
        content.setDisable(value);
    }

    private void showNotice(String text) {
        noticeLabel.setText(text);
        setShown(noticeLabel, !text.isEmpty());
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private void showShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard shortcuts");
        alert.setHeaderText("Add loan");
        alert.setContentText("""
                Tab        Next field
                Ctrl+S     Add loan
                Esc        Cancel and go back
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
