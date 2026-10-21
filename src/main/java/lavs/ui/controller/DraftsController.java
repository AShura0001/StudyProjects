package lavs.ui.controller;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import lavs.business.model.CoBorrowerType;
import lavs.business.model.CustomerPayloads;
import lavs.business.model.CustomerReference;
import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;
import lavs.business.model.DraftType;
import lavs.business.model.LoanDetails;
import lavs.business.model.LoanPayloads;
import lavs.business.service.BackgroundSync;
import lavs.ui.LavsApp;
import lavs.ui.Navigator;
import lavs.ui.component.Choices;
import lavs.ui.component.Format;
import lavs.ui.sync.SyncStatus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * The staff member's saved changes (drafts): what is waiting to be sent, what needs
 * checking, and what went through this week. Opened from the sync dots on any screen,
 * in its own window so the screen underneath keeps its place.
 */
public class DraftsController {

    private static final DateTimeFormatter SAVED_AT =
            DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private static Stage openWindow;

    @FXML private Parent root;
    @FXML private Node content;
    @FXML private VBox listBox;
    @FXML private Label summaryLabel;
    @FXML private Label noticeLabel;
    @FXML private ProgressIndicator progress;

    private final BackgroundSync drafts = LavsApp.context().drafts();
    private Stage window;
    private Stage owner;

    /**
     * Opens the window, or brings it to the front if it is already open.
     *
     * @return false when nobody is logged in, so there are no drafts to show
     */
    public static boolean openWindow(Stage owner) {
        if (LavsApp.context() == null || LavsApp.context().services().isEmpty()) {
            return false;
        }
        if (openWindow != null && openWindow.isShowing()) {
            openWindow.toFront();
            return true;
        }
        FXMLLoader loader = new FXMLLoader(DraftsController.class.getResource("/lavs/ui/view/drafts.fxml"));
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load the drafts window", e);
        }
        DraftsController controller = loader.getController();
        Stage window = new Stage();
        window.initOwner(owner);
        window.initModality(Modality.WINDOW_MODAL);
        window.setTitle("Saved changes");
        window.setScene(new Scene(root, 680, 540));
        window.setMinWidth(520);
        window.setMinHeight(380);
        window.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), window::close);
        controller.window = window;
        controller.owner = owner;

        // Keep the list current while the window is open: every change in the sync status reloads it.
        SyncStatus status = LavsApp.context().syncStatus();
        ChangeListener<Object> reload = (obs, old, now) -> controller.reload();
        status.stateProperty().addListener(reload);
        status.countProperty().addListener(reload);
        window.setOnHidden(e -> {
            status.stateProperty().removeListener(reload);
            status.countProperty().removeListener(reload);
            openWindow = null;
        });

        openWindow = window;
        window.show();
        controller.reload();
        return true;
    }

    @FXML
    private void initialize() {
        setShown(noticeLabel, false);
        setShown(progress, false);
    }

    @FXML
    private void onClose() {
        window.close();
    }

    @FXML
    private void onSendNow() {
        drafts.requestSync();
        showNotice("Sending anything that is waiting…");
    }

    private void reload() {
        run(drafts::findAll, this::render, "Could not read the saved changes.");
    }

    private void render(List<Draft> all) {
        List<Draft> attention = new ArrayList<>();
        List<Draft> waiting = new ArrayList<>();
        List<Draft> sent = new ArrayList<>();
        for (Draft draft : all) {
            switch (draft.getStatus()) {
                case UNCERTAIN, REJECTED -> attention.add(draft);
                case PENDING, SENDING -> waiting.add(draft);
                case SYNCED -> sent.add(draft);
            }
        }
        sent.sort(Comparator.comparing(Draft::getCreatedAt).reversed());

        summaryLabel.setText(attention.isEmpty() && waiting.isEmpty()
                ? "Everything you saved has been sent to the mainframe."
                : attention.size() + " need checking, " + waiting.size() + " waiting to be sent.");

        listBox.getChildren().clear();
        section("Needs checking", attention, "Nothing needs checking.");
        section("Waiting to be sent", waiting, "Nothing is waiting.");
        section("Sent in the last 7 days", sent, "Nothing sent recently.");
    }

    private void section(String title, List<Draft> drafts, String empty) {
        Label heading = new Label(title);
        heading.getStyleClass().add("draft-section");
        listBox.getChildren().add(heading);
        if (drafts.isEmpty()) {
            Label none = new Label(empty);
            none.getStyleClass().add("list-item-sub");
            listBox.getChildren().add(none);
        }
        for (Draft draft : drafts) {
            listBox.getChildren().add(row(draft));
        }
    }

    // ---- One draft ----

    private Node row(Draft draft) {
        Label title = new Label(title(draft));
        title.getStyleClass().add("list-item-title");
        title.setWrapText(true);
        Label status = new Label(statusText(draft));
        status.getStyleClass().addAll("address-tag", statusStyle(draft.getStatus()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(8, title, spacer, status);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(4, top);
        card.getStyleClass().add("draft-row");
        for (String line : details(draft)) {
            Label detail = new Label(line);
            detail.getStyleClass().add("list-item-sub");
            detail.setWrapText(true);
            card.getChildren().add(detail);
        }
        for (String error : draft.getErrors().values()) {
            Label problem = new Label(error);
            problem.getStyleClass().add("draft-error");
            problem.setWrapText(true);
            card.getChildren().add(problem);
        }
        HBox actions = actions(draft);
        if (!actions.getChildren().isEmpty()) {
            card.getChildren().add(actions);
        }
        return card;
    }

    private static String title(Draft draft) {
        try {
            return switch (draft.getType()) {
                case LOAN -> {
                    var loan = LoanPayloads.readLoan(draft.getPayload());
                    yield "New loan of " + Format.money(loan.details().principal()) + " for " + who(loan.customer());
                }
                case CUSTOMER -> {
                    var customer = CustomerPayloads.readCustomer(draft.getPayload()).customer();
                    yield "New customer " + Format.name(customer);
                }
                case COBORROWER -> {
                    var person = LoanPayloads.readCoBorrower(draft.getPayload());
                    yield (person.type() == CoBorrowerType.GUARANTOR ? "Guarantor " : "Co-borrower ")
                            + who(person.customer()) + " on a new loan";
                }
            };
        } catch (IllegalArgumentException unreadable) {
            return "A saved " + draft.getType().name().toLowerCase(Locale.ROOT) + " that could not be read";
        }
    }

    private static String who(CustomerReference customer) {
        return customer instanceof CustomerReference.ExistingCustomer existing
                ? existing.customerNumber() : "a new customer (not sent yet)";
    }

    private static List<String> details(Draft draft) {
        List<String> lines = new ArrayList<>();
        lines.add("Saved " + SAVED_AT.format(draft.getCreatedAt()));
        if (draft.getType() == DraftType.LOAN) {
            try {
                LoanDetails d = LoanPayloads.readLoan(draft.getPayload()).details();
                lines.add(d.rateAmount().stripTrailingZeros().toPlainString() + "% " + Choices.label(d.rateType())
                        + " (" + Choices.label(d.rateFrequency()) + ")  •  " + Format.money(d.paymentAmount()) + " "
                        + Choices.label(d.paymentFrequency()) + "  •  " + d.startDate() + " to " + d.renewalDate());
            } catch (IllegalArgumentException ignored) {
                // the title already says it could not be read
            }
        }
        draft.getServerNumber().ifPresent(n -> lines.add("Mainframe number " + n));
        draft.checkpoint("customer").filter(n -> draft.getStatus() != DraftStatus.SYNCED).ifPresent(n -> lines.add(
                "Customer " + n + " already exists on the mainframe. Sending again carries on from where it stopped."));
        if (draft.getStatus() == DraftStatus.PENDING && !draft.getDependsOn().isEmpty()) {
            lines.add("Waits for the record it belongs to to be sent first.");
        }
        if (draft.getStatus() == DraftStatus.UNCERTAIN) {
            lines.add("The connection dropped while this was being sent, so it may or may not have reached the "
                    + "mainframe. Check the mainframe, then choose below. Sending it again blindly could create a copy.");
        }
        return lines;
    }

    private static String statusText(Draft draft) {
        return switch (draft.getStatus()) {
            case PENDING -> "WAITING";
            case SENDING -> "SENDING";
            case UNCERTAIN -> "CHECK";
            case REJECTED -> "REFUSED";
            case SYNCED -> "SENT";
        };
    }

    private static String statusStyle(DraftStatus status) {
        return switch (status) {
            case PENDING, SENDING -> "draft-tag-waiting";
            case UNCERTAIN, REJECTED -> "draft-tag-attention";
            case SYNCED -> "address-tag-mailing";
        };
    }

    private HBox actions(Draft draft) {
        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_RIGHT);
        String id = draft.getLocalId();
        boolean loan = draft.getType() == DraftType.LOAN;
        switch (draft.getStatus()) {
            case UNCERTAIN -> {
                actions.getChildren().add(button("It reached the mainframe…", () -> confirmReached(id, loan)));
                actions.getChildren().add(button("It did not, send again", () -> confirmNotSent(id)));
            }
            case REJECTED, PENDING -> {
                if (loan) {
                    actions.getChildren().add(button(draft.getStatus() == DraftStatus.REJECTED ? "Fix and resend" : "Edit",
                            () -> fixLoan(draft)));
                }
                actions.getChildren().add(button("Discard", () -> discard(id)));
            }
            default -> { }
        }
        return actions;
    }

    private static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("btn-edit-chip");
        button.setOnAction(e -> action.run());
        return button;
    }

    private void confirmReached(String id, boolean loan) {
        TextInputDialog ask = new TextInputDialog();
        ask.initOwner(window);
        ask.setTitle("It reached the mainframe");
        ask.setHeaderText("Find the record on the mainframe and enter the number it was given.");
        ask.setContentText(loan ? "Loan number (e.g. LN-5004):" : "Number:");
        Optional<String> number = ask.showAndWait().map(String::trim).filter(n -> !n.isEmpty());
        number.ifPresent(n -> change(() -> drafts.confirmSynced(id, n), "Marked as sent."));
    }

    private void confirmNotSent(String id) {
        if (confirm("Only do this after checking the mainframe does not have it. It will be sent again.")) {
            change(() -> drafts.confirmNotSent(id), "It will be sent again.");
        }
    }

    private void discard(String id) {
        if (confirm("Discard this saved change? It will never be sent, and this cannot be undone.")) {
            change(() -> {
                drafts.discard(id);
                return null;
            }, "Discarded.");
        }
    }

    /** Opens the Add loan form on this draft, in the main window. */
    private void fixLoan(Draft draft) {
        CustomerReference customer;
        try {
            customer = LoanPayloads.readLoan(draft.getPayload()).customer();
        } catch (IllegalArgumentException e) {
            showNotice("This loan could not be read, so it can only be discarded.");
            return;
        }
        Runnable back = customer instanceof CustomerReference.ExistingCustomer existing
                ? () -> Navigator.<CustomerDetailsController>show("customerdetails",
                        c -> c.open(existing.customerNumber(), () -> Navigator.show("customersearch")))
                : () -> Navigator.show("customersearch");
        window.close();
        Navigator.<AddLoanController>show("addloan", a -> a.openToFix(draft.getLocalId(), back));
    }

    private boolean confirm(String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.setTitle("Saved changes");
        alert.initOwner(window);
        return alert.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    // ---- Running work off the JavaFX thread ----

    private void change(Callable<?> work, String doneText) {
        run(work, result -> {
            showNotice(doneText);
            reload();
        }, "That change could not be made.");
    }

    private <T> void run(Callable<T> work, java.util.function.Consumer<T> then, String fallback) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            then.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable error = task.getException();
            showNotice(error instanceof IllegalStateException || error instanceof IllegalArgumentException
                    ? error.getMessage() : Format.problem(error, fallback));
        });
        setBusy(true);
        Thread worker = new Thread(task, "drafts");
        worker.setDaemon(true);
        worker.start();
    }

    private void setBusy(boolean busy) {
        setShown(progress, busy);
        content.setDisable(busy);
    }

    private void showNotice(String text) {
        noticeLabel.setText(text);
        setShown(noticeLabel, !text.isEmpty());
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }
}
