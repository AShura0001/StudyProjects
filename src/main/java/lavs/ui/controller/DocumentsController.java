package lavs.ui.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import lavs.business.model.StoredDocument;
import lavs.business.repository.RecordRejectedException;
import lavs.business.service.DocumentService;
import lavs.ui.LavsApp;
import lavs.ui.Navigator;
import lavs.ui.component.Format;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Every document recorded against one customer or one loan, with a way to add and remove them. */
public class DocumentsController {

    /** Whose documents are shown. */
    public enum Owner {
        CUSTOMER("Customer documents", "customer"),
        LOAN("Loan documents", "loan");

        private final String title;
        private final String word;

        Owner(String title, String word) {
            this.title = title;
            this.word = word;
        }
    }

    @FXML private BorderPane root;
    @FXML private Node content;
    @FXML private ProgressIndicator progress;
    @FXML private Label titleLabel;
    @FXML private Label ownerIdLabel;
    @FXML private VBox documentsBox;
    @FXML private Label noticeLabel;

    private final DocumentService documents = LavsApp.context().documents();
    private Owner owner = Owner.CUSTOMER;
    private String ownerNumber;
    private Runnable onBack = () -> Navigator.show("customersearch");
    private boolean busy;

    @FXML
    private void initialize() {
        setShown(noticeLabel, false);
        setShown(progress, false);
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerAccelerators(scene);
                Platform.runLater(root::requestFocus);
            }
        });
    }

    /**
     * Shows a customer's documents.
     *
     * @param back what Back does, so the user returns to the customer
     */
    public void openForCustomer(String customerNumber, Runnable back) {
        open(Owner.CUSTOMER, customerNumber, back);
    }

    /**
     * Shows a loan's documents.
     *
     * @param back what Back does, so the user returns to the loan
     */
    public void openForLoan(String loanNumber, Runnable back) {
        open(Owner.LOAN, loanNumber, back);
    }

    private void open(Owner owner, String number, Runnable back) {
        this.owner = owner;
        this.ownerNumber = number;
        this.onBack = back;
        titleLabel.setText(owner.title);
        ownerIdLabel.setText(number);
        reload("");
    }

    private void reload(String noticeWhenDone) {
        Owner whose = owner;
        String number = ownerNumber;
        Task<List<StoredDocument>> task = new Task<>() {
            @Override
            protected List<StoredDocument> call() {
                return whose == Owner.CUSTOMER ? documents.documentsOfCustomer(number) : documents.documentsOfLoan(number);
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            render(task.getValue());
            showNotice(noticeWhenDone);
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showNotice(Format.problem(task.getException(), "Could not reach the mainframe. Try again."));
        });
        setBusy(true);
        startWorker(task, "documents");
    }

    private void render(List<StoredDocument> list) {
        documentsBox.getChildren().clear();
        if (list.isEmpty()) {
            Label none = new Label("No documents recorded for this " + owner.word + " yet.");
            none.getStyleClass().add("notes-empty");
            documentsBox.getChildren().add(none);
            return;
        }
        for (StoredDocument document : list) {
            documentsBox.getChildren().add(card(document));
        }
    }

    /** "120 KB", "1.4 MB" or "860 bytes"; empty when the size is not known. */
    static String sizeOf(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "";
        }
        if (bytes < 1024) {
            return bytes + " bytes";
        }
        if (bytes < 1024 * 1024) {
            return Math.round(bytes / 1024.0) + " KB";
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /** "payslip.pdf  •  120 KB  •  application/pdf", leaving out what was not sent. */
    static String detailsOf(StoredDocument document) {
        StringBuilder details = new StringBuilder(Format.text(document.fileName()));
        for (String part : new String[] {sizeOf(document.size()), Format.text(document.fileType())}) {
            if (!part.isEmpty()) {
                details.append(details.isEmpty() ? "" : "  •  ").append(part);
            }
        }
        return details.toString();
    }

    private Node card(StoredDocument document) {
        String kind = Format.text(document.type());
        Label title = new Label(kind.isEmpty() ? "Document" : kind);
        title.getStyleClass().add("note-card-title");
        title.setMinWidth(0);

        Label details = new Label(detailsOf(document));
        details.getStyleClass().add("note-card-meta");
        details.setMinWidth(0);

        VBox heading = new VBox(1, title, details);
        HBox.setHgrow(heading, Priority.ALWAYS);
        heading.setMinWidth(0);

        Button remove = new Button();
        remove.getStyleClass().add("btn-remove");
        SVGPath cross = new SVGPath();
        cross.setContent("M5 5 L19 19 M19 5 L5 19");
        cross.getStyleClass().add("remove-icon");
        remove.setGraphic(cross);
        remove.setMinWidth(Region.USE_PREF_SIZE);
        remove.setOnAction(e -> remove(document));

        HBox top = new HBox(6, heading, remove);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(6, top);
        card.getStyleClass().add("note-card");
        return card;
    }

    private void remove(StoredDocument document) {
        if (busy) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove the record of " + Format.text(document.fileName()) + "? This cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Remove document");
        alert.setHeaderText(null);
        alert.initOwner(root.getScene().getWindow());
        Optional<ButtonType> answer = alert.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.YES) {
            return;
        }

        Owner whose = owner;
        String number = ownerNumber;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                if (whose == Owner.CUSTOMER) {
                    documents.removeFromCustomer(number, document.id());
                } else {
                    documents.removeFromLoan(number, document.id());
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> reload("Document removed."));
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable error = task.getException();
            showNotice(error instanceof RecordRejectedException refused
                    ? refused.getMessage()
                    : Format.problem(error, "Could not remove the document. Try again."));
        });
        showNotice("");
        setBusy(true);
        startWorker(task, "document-remove");
    }

    @FXML
    private void onAddDocument() {
        if (busy || ownerNumber == null) {
            return;
        }
        Owner whose = owner;
        String number = ownerNumber;
        Runnable back = onBack;
        Runnable reopen = () -> Navigator.<DocumentsController>show("documents", d -> d.open(whose, number, back));
        Navigator.<AddDocumentController>show("adddocument", a -> a.open(whose, number, reopen, reopen));
    }

    @FXML
    private void onBack() {
        if (!busy) {
            onBack.run();
        }
    }

    @FXML
    private void onLogout() {
        if (!busy) {
            LavsApp.context().logout();
            Navigator.show("loggedout");
        }
    }

    /** Each accelerator is a second route to a control that is already on screen. */
    private void registerAccelerators(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::onBack);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), this::onAddDocument);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN), this::onLogout);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), this::showShortcuts);
    }

    private void setBusy(boolean value) {
        busy = value;
        setShown(progress, value);
        content.setOpacity(value ? 0.35 : 1.0);
        content.setDisable(value);
    }

    private static void startWorker(Task<?> task, String name) {
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
        alert.setHeaderText(owner.title);
        alert.setContentText("""
                Ctrl+N     Record a document
                Esc        Back
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
