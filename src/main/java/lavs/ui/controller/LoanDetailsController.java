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
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
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

import lavs.ui.LavsApp;
import lavs.ui.Navigator;
import lavs.ui.stub.StubLoanService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loan Details with a view mode and an edit mode. Edit changes are held in the
 * form until Save; Cancel (or leaving) discards them after confirmation.
 */
public class LoanDetailsController {

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
    @FXML private TextField startDateField;
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
    @FXML private Node editActions;
    @FXML private Label noticeLabel;

    private final StubLoanService loanService = new StubLoanService();
    private final Map<String, TextField> fields = new LinkedHashMap<>();
    private Runnable onBack = () -> Navigator.show("customersearch");

    private StubLoanService.LoanDetails details;
    private List<StubLoanService.CoBorrower> workingCoBorrowers = new ArrayList<>();
    private boolean editing;

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
        setBusy(true);

        Task<StubLoanService.LoanDetails> task = new Task<>() {
            @Override
            protected StubLoanService.LoanDetails call() {
                return loanService.details(loanNumber);
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false);
            details = task.getValue();
            if (details == null) {
                showNotice("Could not find loan " + loanNumber + ".");
            } else {
                revert();
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showNotice("Could not reach the mainframe. Try again.");
        });

        startWorker(task, "loan-details");
    }

    /** Puts the form, schedule and co-borrowers back to the last saved values. */
    private void revert() {
        loanNameField.setText(details.loanName());
        loanAmountField.setText(details.loanAmount());
        interestRateField.setText(details.interestRate());
        loanTermField.setText(details.loanTerm());
        startDateField.setText(details.startDate());
        maturityDateField.setText(details.maturityDate());
        monthlyPaymentField.setText(details.monthlyPayment());
        outstandingBalanceField.setText(details.outstandingBalance());
        totalPaidField.setText(details.totalPaid());
        principalPaidField.setText(details.principalPaid());
        interestPaidField.setText(details.interestPaid());
        remainingBalanceField.setText(details.remainingBalance());
        nextPaymentDueField.setText(details.nextPaymentDue());

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
    }

    private boolean isDirty() {
        return details != null && !details.equals(readForm());
    }

    private StubLoanService.LoanDetails readForm() {
        return new StubLoanService.LoanDetails(details.loanNumber(),
                text(loanNameField), text(loanAmountField), text(interestRateField),
                text(loanTermField), text(startDateField), text(maturityDateField),
                text(monthlyPaymentField), text(outstandingBalanceField),
                details.statusText(), details.paymentsMade(), details.paymentsTotal(),
                text(totalPaidField), text(principalPaidField), text(interestPaidField),
                text(remainingBalanceField), text(nextPaymentDueField),
                List.copyOf(workingCoBorrowers), details.schedule());
    }

    private static String text(TextField field) {
        return field.getText().trim();
    }

    private void renderSchedule() {
        scheduleHeaderBox.getChildren().setAll(scheduleRow(null));
        scheduleBox.getChildren().clear();
        if (details == null) {
            return;
        }
        for (StubLoanService.ScheduleEntry entry : details.schedule()) {
            scheduleBox.getChildren().add(scheduleRow(entry));
        }
    }

    /** A null entry builds the header row. */
    private GridPane scheduleRow(StubLoanService.ScheduleEntry entry) {
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
                edit.setOnAction(e -> showNotice("Editing a payment has not been built yet."));
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

    private void renderCoBorrowers() {
        coBorrowersBox.getChildren().clear();
        if (details == null) {
            return;
        }
        for (StubLoanService.CoBorrower coBorrower : workingCoBorrowers) {
            coBorrowersBox.getChildren().add(coBorrowerRow(coBorrower));
        }
    }

    private Node coBorrowerRow(StubLoanService.CoBorrower coBorrower) {
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

    private void openCoBorrower(StubLoanService.CoBorrower coBorrower) {
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
        for (TextField field : fields.values()) {
            field.setEditable(edit);
            field.setFocusTraversable(edit);
        }

        toggleStyle(content, "editing", edit);
        toggleStyle(viewModeButton, "mode-selected", !edit);
        toggleStyle(editModeButton, "mode-selected", edit);
        setShown(scheduleEditButton, edit);
        setShown(addCoBorrowerButton, edit);
        setShown(editActions, edit);
        clearFieldErrors();
        if (details != null) {
            renderSchedule();
            renderCoBorrowers();
        }
        if (edit) {
            Platform.runLater(loanNameField::requestFocus);
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
        if (details != null && !editing) {
            showNotice("");
            applyMode(true);
        }
    }

    @FXML
    private void onViewMode() {
        if (editing && confirmDiscard()) {
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
        if (!editing || details == null) {
            return;
        }
        StubLoanService.LoanDetails edited = readForm();
        clearFieldErrors();
        setBusy(true);

        Task<Map<String, String>> task = new Task<>() {
            @Override
            protected Map<String, String> call() {
                return loanService.save(edited);
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false);
            Map<String, String> errors = task.getValue();
            if (errors.isEmpty()) {
                details = edited;
                revert();
                applyMode(false);
                showNotice("Changes saved.");
            } else {
                showFieldErrors(errors);
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showNotice("Could not reach the mainframe. Your changes are still on screen.");
        });

        startWorker(task, "loan-save");
    }

    private void showFieldErrors(Map<String, String> errors) {
        TextField first = null;
        for (String key : errors.keySet()) {
            TextField field = fields.get(key);
            if (field != null) {
                toggleStyle(field, "field-error", true);
                if (first == null) {
                    first = field;
                }
            }
        }
        showNotice(String.join("  ", errors.values()));
        if (first != null) {
            first.requestFocus();
        }
    }

    private void clearFieldErrors() {
        for (TextField field : fields.values()) {
            toggleStyle(field, "field-error", false);
        }
    }

    @FXML
    private void onScheduleEdit() {
        showNotice("Editing the repayment schedule has not been built yet.");
    }

    @FXML
    private void onAddCoBorrower() {
        showNotice("Adding a co-borrower has not been built yet.");
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
