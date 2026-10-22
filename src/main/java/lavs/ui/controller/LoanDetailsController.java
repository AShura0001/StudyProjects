package lavs.ui.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import lavs.ui.Navigator;
import lavs.ui.LavsApp;
import lavs.business.model.CoBorrower;
import lavs.business.model.LoanDetails;
import lavs.business.model.LoanRecord;
import lavs.business.model.RepaymentEntry;
import lavs.business.model.RepaymentProgress;
import lavs.business.model.RepaymentStatus;
import lavs.business.service.LoanAccountService;
import lavs.ui.component.Format;
import lavs.ui.component.Inputs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Loan Details with a view mode and an edit mode, read and saved through
 * LoanAccountService. Edit changes are held in the form until Save; Cancel (or
 * leaving) discards them after confirmation.
 */
public class LoanDetailsController {

    /** What the screen shows, worked out from the business models. */
    private record View(String loanNumber, String loanName, String loanAmount,
                        String interestRate, String loanTerm, String startDate,
                        String maturityDate, String monthlyPayment, String outstandingBalance,
                        String statusText, int paymentsMade, int paymentsTotal,
                        String totalPaid, String principalPaid, String interestPaid,
                        String remainingBalance, String nextPaymentDue,
                        List<CoBorrowerRow> coBorrowers, List<ScheduleRow> schedule) {
    }

    private record ScheduleRow(int number, String dueDate, String principal, String interest,
                               String total, String status) {
    }

    private record CoBorrowerRow(long id, String name, String customerNumber) {
    }

    private static final double[] VIEW_COLUMNS = {15, 19, 17, 17, 16, 16};
    private static final double[] EDIT_COLUMNS = {13, 16, 14, 14, 13, 13, 17};

    @FXML private BorderPane root;
    @FXML private Node content;
    @FXML private ProgressIndicator progress;
    @FXML private Label loanIdLabel;
    @FXML private TextField loanNameField;
    @FXML private TextField loanAmountField;
    @FXML private TextField interestRateField;
    @FXML private TextField loanTermField;
    @FXML private DatePicker startDateField;
    @FXML private TextField maturityDateField;
    @FXML private TextField monthlyPaymentField;
    @FXML private TextField outstandingBalanceField;
    @FXML private TextField totalPaidField;
    @FXML private TextField principalPaidField;
    @FXML private TextField interestPaidField;
    @FXML private TextField remainingBalanceField;
    @FXML private TextField nextPaymentDueField;
    @FXML private Label statusLabel;
    @FXML private Label statusSubtitleLabel;
    @FXML private ProgressBar paymentsProgress;
    @FXML private VBox scheduleHeaderBox;
    @FXML private VBox scheduleBox;
    @FXML private VBox coBorrowersBox;
    @FXML private Button scheduleEditButton;
    @FXML private Button addCoBorrowerButton;
    @FXML private Button viewModeButton;
    @FXML private Button editModeButton;
    @FXML private Button submitButton;
    @FXML private Node editActions;
    @FXML private Label noticeLabel;

    private final LoanAccountService loanAccounts = LavsApp.context().loanAccounts();
    private final Map<String, Control> fields = new LinkedHashMap<>();
    /** The only fields typed into; the rest are worked out from the loan and stay read-only. */
    private static final java.util.Set<String> EDITABLE = java.util.Set.of("loanAmount", "startDate");
    private Runnable onBack = () -> Navigator.show("customersearch");

    private View details;
    private LoanRecord loan;
    private List<CoBorrowerRow> workingCoBorrowers = new ArrayList<>();
    private boolean editing;
    private boolean busy;

    @FXML
    private void initialize() {
        fields.put("loanName", loanNameField);
        fields.put("loanAmount", loanAmountField);
        fields.put("interestRate", interestRateField);
        fields.put("loanTerm", loanTermField);
        fields.put("startDate", startDateField);
        fields.put("maturityDate", maturityDateField);
        fields.put("monthlyPayment", monthlyPaymentField);
        fields.put("outstandingBalance", outstandingBalanceField);
        fields.put("totalPaid", totalPaidField);
        fields.put("principalPaid", principalPaidField);
        fields.put("interestPaid", interestPaidField);
        fields.put("remainingBalance", remainingBalanceField);
        fields.put("nextPaymentDue", nextPaymentDueField);

        Inputs.number(loanAmountField, 2);
        Inputs.date(startDateField);
        Tooltip scheduleTip = new Tooltip("Change this with Edit schedule.");
        Tooltip workedOutTip = new Tooltip("Worked out from the loan's figures.");
        fields.forEach((key, field) -> {
            if (!EDITABLE.contains(key)) {
                field.setTooltip(key.equals("maturityDate") || key.equals("monthlyPayment") ? scheduleTip : workedOutTip);
            }
        });

        setShown(noticeLabel, false);
        setShown(progress, false);
        applyMode(false);
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerAccelerators(scene);
                Platform.runLater(root::requestFocus);
            }
        });
    }

    /**
     * Called by the previous screen before this one is displayed.
     *
     * @param back what Back should do, so the user returns to where they came from
     */
    public void open(String loanNumber, Runnable back) {
        onBack = back;
        loanIdLabel.setText(loanNumber);
        load(loanNumber, "");
    }

    private void load(String loanNumber, String noticeWhenDone) {
        setBusy(true);
        Task<Object[]> task = new Task<>() {
            @Override
            protected Object[] call() {
                LoanRecord found = loanAccounts.find(loanNumber).orElse(null);
                return found == null ? null : new Object[] {found, read(found)};
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false);
            Object[] result = task.getValue();
            if (result == null) {
                showNotice("Could not find loan " + loanNumber + ".");
            } else {
                loan = (LoanRecord) result[0];
                details = (View) result[1];
                revert();
                showNotice(noticeWhenDone);
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showNotice(Format.problem(task.getException(), "Could not reach the mainframe. Try again."));
        });

        startWorker(task, "loan-details");
    }

    /** Runs off the JavaFX thread: the loan's schedule, payments and co-borrowers, turned into screen text. */
    private View read(LoanRecord loan) {
        String loanNumber = loan.loanNumber();
        // Which repayments are paid, and the totals, are worked out by the business layer.
        RepaymentProgress progress = loanAccounts.progressOf(loan);
        List<ScheduleRow> rows = new ArrayList<>();
        for (RepaymentProgress.Repayment repayment : progress.repayments()) {
            RepaymentEntry entry = repayment.entry();
            rows.add(new ScheduleRow((int) entry.paymentNumber(), Format.date(entry.date()),
                    Format.money(entry.principalAmount()), Format.money(entry.interestAmount()),
                    Format.money(entry.amountPaid()), statusLabel(repayment.status())));
        }
        BigDecimal principal = loan.principal() == null ? BigDecimal.ZERO : loan.principal();
        String nextDue = progress.nextDueDate().map(Format::date).orElse("None");

        List<CoBorrowerRow> people = loanAccounts.coBorrowersOf(loanNumber).stream()
                .map(c -> new CoBorrowerRow(c.id(),
                        c.coBorrowerName() == null ? c.coBorrowerNumber() : c.coBorrowerName(), c.coBorrowerNumber()))
                .toList();

        String rate = loan.rateAmount() == null ? ""
                : String.format(Locale.US, "%.2f%% %s (%s)", loan.rateAmount(),
                Format.text(loan.rateType()), Format.text(loan.rateFrequency()));
        String payment = loan.paymentAmount() == null ? ""
                : Format.money(loan.paymentAmount()) + " / " + Format.text(loan.paymentFrequency());
        return new View(loanNumber, Format.loanName(loanNumber), Format.money(principal), rate,
                term(loan.startDate(), loan.renewalDate()), Format.date(loan.startDate()),
                Format.date(loan.renewalDate()), payment, Format.money(progress.outstanding()),
                loan.status() == null ? "Unknown" : loan.status(), progress.paymentsMade(), progress.paymentsTotal(),
                Format.money(progress.totalPaid()), Format.money(progress.principalPaid()),
                Format.money(progress.interestPaid()), Format.money(progress.outstanding()), nextDue, people,
                List.copyOf(rows));
    }

    private static String statusLabel(RepaymentStatus status) {
        return switch (status) {
            case PAID -> "Paid";
            case DUE -> "Due";
            case UPCOMING -> "Upcoming";
        };
    }

    private static String term(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return "";
        }
        Period period = Period.between(start, end);
        StringBuilder text = new StringBuilder();
        if (period.getYears() > 0) {
            text.append(period.getYears()).append(period.getYears() == 1 ? " year" : " years");
        }
        if (period.getMonths() > 0) {
            text.append(text.isEmpty() ? "" : " ").append(period.getMonths()).append(period.getMonths() == 1 ? " month" : " months");
        }
        if (text.isEmpty()) {
            text.append(period.getDays()).append(" days");
        }
        return text.toString();
    }

    /** Puts the form, schedule and co-borrowers back to the last saved values. */
    private void revert() {
        Inputs.setValue(loanNameField, details.loanName());
        Inputs.setValue(loanAmountField, details.loanAmount());
        Inputs.setValue(interestRateField, details.interestRate());
        Inputs.setValue(loanTermField, details.loanTerm());
        Inputs.setValue(startDateField, details.startDate());
        Inputs.setValue(maturityDateField, details.maturityDate());
        Inputs.setValue(monthlyPaymentField, details.monthlyPayment());
        Inputs.setValue(outstandingBalanceField, details.outstandingBalance());
        Inputs.setValue(totalPaidField, details.totalPaid());
        Inputs.setValue(principalPaidField, details.principalPaid());
        Inputs.setValue(interestPaidField, details.interestPaid());
        Inputs.setValue(remainingBalanceField, details.remainingBalance());
        Inputs.setValue(nextPaymentDueField, details.nextPaymentDue());

        statusLabel.setText(details.statusText());
        int percent = details.paymentsTotal() == 0 ? 0
                : Math.round(100f * details.paymentsMade() / details.paymentsTotal());
        statusSubtitleLabel.setText(details.paymentsMade() + " of " + details.paymentsTotal()
                + " payments completed (" + percent + "%)");
        paymentsProgress.setProgress(details.paymentsTotal() == 0 ? 0
                : (double) details.paymentsMade() / details.paymentsTotal());

        workingCoBorrowers = new ArrayList<>(details.coBorrowers());
        clearFieldErrors();
        renderSchedule();
        renderCoBorrowers();
        updateSubmitButton();
    }

    /** Only a New loan can be sent for checking, and not while it is being edited. */
    private void updateSubmitButton() {
        setShown(submitButton, !editing && loan != null
                && LoanAccountService.NEW_STATUS.equalsIgnoreCase(loan.status()));
    }

    private boolean isDirty() {
        return details != null && !details.equals(readForm());
    }

    private View readForm() {
        return new View(details.loanNumber(),
                text(loanNameField), text(loanAmountField), text(interestRateField),
                text(loanTermField), text(startDateField), text(maturityDateField),
                text(monthlyPaymentField), text(outstandingBalanceField),
                details.statusText(), details.paymentsMade(), details.paymentsTotal(),
                text(totalPaidField), text(principalPaidField), text(interestPaidField),
                text(remainingBalanceField), text(nextPaymentDueField),
                List.copyOf(workingCoBorrowers), details.schedule());
    }

    private static String text(Control field) {
        return Inputs.valueOf(field);
    }

    private void renderSchedule() {
        scheduleHeaderBox.getChildren().setAll(scheduleRow(null));
        scheduleBox.getChildren().clear();
        if (details == null) {
            return;
        }
        for (ScheduleRow entry : details.schedule()) {
            scheduleBox.getChildren().add(scheduleRow(entry));
        }
    }

    /** A null entry builds the header row. */
    private GridPane scheduleRow(ScheduleRow entry) {
        boolean header = entry == null;
        GridPane row = new GridPane();
        row.getStyleClass().add(header ? "schedule-header-row" : "schedule-row");
        row.setMinHeight(header ? 20 : 22);
        row.setPrefHeight(header ? 20 : 22);
        row.setAlignment(Pos.CENTER_LEFT);

        for (double width : editing ? EDIT_COLUMNS : VIEW_COLUMNS) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width);
            row.getColumnConstraints().add(column);
        }

        String[] values = header
                ? new String[] {"Payment #", "Due Date", "Principal", "Interest", "Total", "Status"}
                : new String[] {String.valueOf(entry.number()), entry.dueDate(), entry.principal(),
                        entry.interest(), entry.total(), entry.status()};
        String[] styles = header
                ? new String[] {"sched-head", "sched-head", "sched-head", "sched-head", "sched-head", "sched-head"}
                : new String[] {"sched-strong", "sched-muted", "sched-strong", "sched-muted",
                        "sched-strong", statusStyle(entry.status())};

        for (int i = 0; i < values.length; i++) {
            Label cell = new Label(values[i]);
            cell.getStyleClass().add(styles[i]);
            row.add(cell, i, 0);
        }

        if (editing) {
            if (header) {
                Label action = new Label("Action");
                action.getStyleClass().add("sched-head");
                row.add(action, values.length, 0);
            } else {
                Button edit = new Button("Edit");
                edit.getStyleClass().add("btn-edit-chip");
                edit.setOnAction(e -> openPaymentEditor(entry));
                row.add(edit, values.length, 0);
            }
        }
        return row;
    }

    private static String statusStyle(String status) {
        return switch (status) {
            case "Due" -> "sched-due";
            case "Upcoming" -> "sched-upcoming";
            default -> "sched-paid";
        };
    }

    private void openPaymentEditor(ScheduleRow entry) {
        if (!confirmDiscard()) {
            return;
        }
        String loanNumber = details.loanNumber();
        Runnable reopen = reopenLoan(loanNumber);
        Navigator.<EditPaymentController>show("editpayment",
                p -> p.open(loanNumber, entry.number(), entry.dueDate(), entry.total(), reopen, reopen));
    }

    private Runnable reopenLoan(String loanNumber) {
        Runnable backFromLoan = onBack;
        return () -> Navigator.<LoanDetailsController>show("loandetails", l -> l.open(loanNumber, backFromLoan));
    }

    private void renderCoBorrowers() {
        coBorrowersBox.getChildren().clear();
        if (details == null) {
            return;
        }
        for (CoBorrowerRow coBorrower : workingCoBorrowers) {
            coBorrowersBox.getChildren().add(coBorrowerRow(coBorrower));
        }
    }

    private Node coBorrowerRow(CoBorrowerRow coBorrower) {
        Label name = new Label(coBorrower.name());
        name.getStyleClass().add("cb-name");
        name.setMinWidth(82);
        name.setPrefWidth(82);

        Label id = new Label(coBorrower.customerNumber());
        id.getStyleClass().add("cb-id");
        id.setMinWidth(52);

        Hyperlink link = new Hyperlink("Click here for more details");
        link.getStyleClass().add("list-item-link");
        link.setOnAction(e -> openCoBorrower(coBorrower));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(4, name);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("cb-row");
        row.setPadding(new Insets(7, 4, 7, 8));

        if (editing) {
            Button remove = new Button();
            remove.getStyleClass().add("btn-remove");
            SVGPath cross = new SVGPath();
            cross.setContent("M5 5 L19 19 M19 5 L5 19");
            cross.getStyleClass().add("remove-icon");
            remove.setGraphic(cross);
            remove.setOnAction(e -> {
                workingCoBorrowers.remove(coBorrower);
                renderCoBorrowers();
            });
            row.getChildren().add(remove);
        }
        row.getChildren().addAll(id, spacer, link);
        return row;
    }

    private void openCoBorrower(CoBorrowerRow coBorrower) {
        if (!confirmDiscard()) {
            return;
        }
        String loanNumber = details.loanNumber();
        Runnable backFromLoan = onBack;
        Navigator.<CustomerDetailsController>show("customerdetails", c -> c.open(coBorrower.customerNumber(),
                () -> Navigator.<LoanDetailsController>show("loandetails", l -> l.open(loanNumber, backFromLoan))));
    }

    /** Switches every editable part of the screen between view and edit mode. */
    private void applyMode(boolean edit) {
        editing = edit;
        fields.forEach((key, field) -> {
            boolean editable = edit && EDITABLE.contains(key);
            Inputs.setLocked(field, !editable);
            toggleStyle(field, "derived", edit && !editable);
        });

        toggleStyle(content, "editing", edit);
        toggleStyle(viewModeButton, "mode-selected", !edit);
        toggleStyle(editModeButton, "mode-selected", edit);
        setShown(scheduleEditButton, edit);
        setShown(addCoBorrowerButton, edit);
        setShown(editActions, edit);
        updateSubmitButton();
        clearFieldErrors();
        if (details != null) {
            renderSchedule();
            renderCoBorrowers();
        }
        if (edit) {
            Platform.runLater(loanAmountField::requestFocus);
        }
    }

    private static void toggleStyle(Node node, String styleClass, boolean on) {
        node.getStyleClass().remove(styleClass);
        if (on) {
            node.getStyleClass().add(styleClass);
        }
    }

    @FXML
    private void onEditMode() {
        if (!busy && details != null && !editing) {
            showNotice("");
            applyMode(true);
        }
    }

    @FXML
    private void onViewMode() {
        if (!busy && editing && confirmDiscard()) {
            revert();
            applyMode(false);
        }
    }

    @FXML
    private void onCancel() {
        onViewMode();
    }

    @FXML
    private void onSave() {
        if (busy || !editing || details == null) {
            return;
        }
        Map<String, String> badDates = Inputs.badDates(fields);
        if (!badDates.isEmpty()) {
            showFieldErrors(badDates);
            return;
        }
        View edited = readForm();
        String amountText = edited.loanAmount().replace("$", "").replace(",", "");
        if (amountText.isEmpty() || edited.startDate().isEmpty()) {
            Map<String, String> missing = new LinkedHashMap<>();
            if (amountText.isEmpty()) {
                missing.put("loanAmount", "Loan amount is required.");
            }
            if (edited.startDate().isEmpty()) {
                missing.put("startDate", "Start date is required.");
            }
            showFieldErrors(missing);
            return;
        }
        LoanDetails current = LoanAccountService.termsOf(loan).orElse(null);
        boolean termsChanged = !edited.loanAmount().equals(details.loanAmount())
                || !edited.startDate().equals(details.startDate());
        if (termsChanged && current == null) {
            showNotice("The mainframe is missing some of this loan's terms, so they cannot be changed here.");
            return;
        }
        LoanDetails terms = termsChanged ? new LoanDetails(new BigDecimal(amountText), current.rateType(),
                current.rateAmount(), current.rateFrequency(), current.paymentAmount(), current.paymentFrequency(),
                LocalDate.parse(edited.startDate()), current.renewalDate()) : null;
        List<Long> removed = details.coBorrowers().stream()
                .filter(c -> !edited.coBorrowers().contains(c))
                .map(CoBorrowerRow::id)
                .toList();
        String loanNumber = details.loanNumber();
        clearFieldErrors();
        setBusy(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                if (terms != null) {
                    loanAccounts.updateTerms(loanNumber, terms);
                }
                for (long id : removed) {
                    loanAccounts.removeCoBorrower(loanNumber, id);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false);
            applyMode(false);
            // Read back what the mainframe now holds, including the schedule worked out again.
            load(loanNumber, "Changes saved.");
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable error = task.getException();
            if (Format.isRuleError(error)) {
                showFieldErrors(Format.fieldErrors(error, Map.of("principal", "loanAmount",
                        "renewalDate", "startDate")));
            } else {
                showNotice(Format.problem(error, "Could not reach the mainframe. Your changes are still on screen."));
            }
        });

        startWorker(task, "loan-save");
    }

    private void showFieldErrors(Map<String, String> errors) {
        Control first = null;
        for (String key : errors.keySet()) {
            Control field = fields.get(key);
            if (field != null) {
                toggleStyle(field, "field-error", true);
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

    private void clearFieldErrors() {
        for (Control field : fields.values()) {
            toggleStyle(field, "field-error", false);
        }
    }

    /** Moves the loan from New to Pending once every rule passes; every problem is listed at once. */
    @FXML
    private void onSubmitForChecking() {
        if (busy || editing || details == null) {
            return;
        }
        String loanNumber = details.loanNumber();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Submit loan " + loanNumber + " for checking? Its status changes from New to Pending.",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Submit for checking");
        alert.setHeaderText(null);
        alert.initOwner(root.getScene().getWindow());
        Optional<ButtonType> answer = alert.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.YES) {
            return;
        }
        showNotice("");
        setBusy(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                loanAccounts.submitForChecking(loanNumber);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false);
            load(loanNumber, "Submitted for checking. The loan is now Pending.");
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable error = task.getException();
            if (Format.isRuleError(error)) {
                showNotice("This loan cannot be submitted yet.  " + Inputs.notice(Format.fieldErrors(error, Map.of())));
            } else {
                // The reply may have been lost after the change was made, so read the loan again before retrying.
                load(loanNumber, Format.problem(error, "Could not reach the mainframe.")
                        + " Check the status before submitting again.");
            }
        });

        startWorker(task, "loan-submit");
    }

    @FXML
    private void onScheduleEdit() {
        if (details == null || !confirmDiscard()) {
            return;
        }
        String loanNumber = details.loanNumber();
        Runnable reopen = reopenLoan(loanNumber);
        Navigator.<EditScheduleController>show("editschedule", s -> s.open(loanNumber, reopen, reopen));
    }

    @FXML
    private void onLoanNotes() {
        if (details == null || !confirmDiscard()) {
            return;
        }
        String loanNumber = details.loanNumber();
        Runnable reopen = reopenLoan(loanNumber);
        Navigator.<LoanNotesController>show("loannotes", n -> n.open(loanNumber, reopen));
    }

    @FXML
    private void onAddCoBorrower() {
        if (details == null || !confirmDiscard()) {
            return;
        }
        String loanNumber = details.loanNumber();
        Runnable backFromLoan = onBack;
        Runnable reopen = () -> Navigator.<LoanDetailsController>show("loandetails",
                l -> l.open(loanNumber, backFromLoan));
        Navigator.<AddCoBorrowerController>show("addcoborrower", a -> a.open(loanNumber, reopen, reopen));
    }

    @FXML
    private void onBack() {
        if (confirmDiscard()) {
            onBack.run();
        }
    }

    @FXML
    private void onLogout() {
        if (confirmDiscard()) {
            LavsApp.context().logout();
            Navigator.show("loggedout");
        }
    }

    /** Esc cancels an edit in progress; otherwise it goes back. */
    private void onEscape() {
        if (editing) {
            onCancel();
        } else {
            onBack();
        }
    }

    /** True when there is nothing to lose or the user agrees to discard their edits. */
    private boolean confirmDiscard() {
        if (!editing || !isDirty()) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "You have unsaved changes. Discard them?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText(null);
        alert.initOwner(root.getScene().getWindow());
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.YES;
    }

    /** Each accelerator is a second route to a control that is already on screen. */
    private void registerAccelerators(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::onEscape);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN), this::onEditMode);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::onSave);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN), this::onLogout);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), this::showShortcuts);
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        setShown(progress, busy);
        content.setOpacity(busy ? 0.35 : 1.0);
        content.setDisable(busy);
    }

    private void startWorker(Task<?> task, String name) {
        Thread worker = new Thread(task, name);
        worker.setDaemon(true);
        worker.start();
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
        alert.setHeaderText("Loan details");
        alert.setContentText("""
                Ctrl+E     Edit mode
                Ctrl+S     Save (in edit mode)
                Esc        Cancel editing, or go back
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
