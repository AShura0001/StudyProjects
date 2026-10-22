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

import lavs.business.model.Relationship;
import lavs.business.model.RelationshipType;
import lavs.business.service.CustomerRelationshipService;
import lavs.business.service.RuleViolationException;
import lavs.ui.LavsApp;
import lavs.ui.Navigator;
import lavs.ui.component.Format;

import java.util.List;
import java.util.Optional;

/** Every relationship of one customer, with a way to add, change and remove them. */
public class RelationshipsController {

    @FXML private BorderPane root;
    @FXML private Node content;
    @FXML private ProgressIndicator progress;
    @FXML private Label customerIdLabel;
    @FXML private VBox relationshipsBox;
    @FXML private Label noticeLabel;

    private final CustomerRelationshipService relationships = LavsApp.context().relationships();
    private String customerNumber;
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
     * Called by the customer's page before this one is displayed.
     *
     * @param back what Back does, so the user returns to the customer
     */
    public void open(String customerNumber, Runnable back) {
        this.customerNumber = customerNumber;
        this.onBack = back;
        customerIdLabel.setText(customerNumber);
        reload("");
    }

    private void reload(String noticeWhenDone) {
        String customer = customerNumber;
        Task<List<Relationship>> task = new Task<>() {
            @Override
            protected List<Relationship> call() {
                return relationships.relationshipsOf(customer);
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
        startWorker(task, "relationships");
    }

    private void render(List<Relationship> list) {
        relationshipsBox.getChildren().clear();
        if (list.isEmpty()) {
            Label none = new Label("No relationships recorded for this customer yet.");
            none.getStyleClass().add("notes-empty");
            relationshipsBox.getChildren().add(none);
            return;
        }
        for (Relationship relationship : list) {
            relationshipsBox.getChildren().add(card(relationship));
        }
    }

    /** "Spouse", or "Other: godparent" for an Other relationship with a description. */
    static String kindOf(Relationship relationship) {
        Optional<RelationshipType> type = RelationshipType.fromMainframeName(relationship.get_type());
        String label = type.map(RelationshipType::label).orElse(Format.text(relationship.get_type()));
        String description = Format.text(relationship.get_otherType());
        return type.orElse(null) == RelationshipType.OTHER && !description.isEmpty() ? label + ": " + description : label;
    }

    /** "Sam Lee  •  01-0001-0000021", or just the number when the name was not sent. */
    static String otherCustomerOf(Relationship relationship) {
        String name = Format.text(relationship.get_secondaryCustomerName());
        String number = Format.text(relationship.get_secondaryCustomerNumber());
        return name.isEmpty() ? number : name + "  •  " + number;
    }

    private Node card(Relationship relationship) {
        Label title = new Label(kindOf(relationship));
        title.getStyleClass().add("note-card-title");
        title.setMinWidth(0);

        Label who = new Label(otherCustomerOf(relationship));
        who.getStyleClass().add("note-card-meta");
        who.setMinWidth(0);

        VBox heading = new VBox(1, title, who);
        HBox.setHgrow(heading, Priority.ALWAYS);
        heading.setMinWidth(0);

        Button edit = new Button("Edit");
        edit.getStyleClass().add("btn-edit-chip");
        edit.setMinWidth(Region.USE_PREF_SIZE);
        edit.setOnAction(e -> openEditor(relationship.get_id()));

        Button remove = new Button();
        remove.getStyleClass().add("btn-remove");
        SVGPath cross = new SVGPath();
        cross.setContent("M5 5 L19 19 M19 5 L5 19");
        cross.getStyleClass().add("remove-icon");
        remove.setGraphic(cross);
        remove.setMinWidth(Region.USE_PREF_SIZE);
        remove.setOnAction(e -> remove(relationship));

        HBox top = new HBox(6, heading, edit, remove);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(6, top);
        card.getStyleClass().add("note-card");
        return card;
    }

    private void remove(Relationship relationship) {
        if (busy) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove this relationship? This cannot be undone.", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Remove relationship");
        alert.setHeaderText(null);
        alert.initOwner(root.getScene().getWindow());
        Optional<ButtonType> answer = alert.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.YES) {
            return;
        }

        String customer = customerNumber;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                relationships.delete(customer, relationship.get_id());
                return null;
            }
        };
        task.setOnSucceeded(e -> reload("Relationship removed."));
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable error = task.getException();
            showNotice(error instanceof RuleViolationException broken
                    ? broken.getMessage()
                    : Format.problem(error, "Could not remove the relationship. Try again."));
        });
        showNotice("");
        setBusy(true);
        startWorker(task, "relationship-remove");
    }

    private void openEditor(Long relationshipId) {
        String customer = customerNumber;
        Runnable back = onBack;
        Runnable reopen = () -> Navigator.<RelationshipsController>show("relationships", r -> r.open(customer, back));
        Navigator.<EditRelationshipController>show("editrelationship",
                e -> e.open(customer, relationshipId, reopen, reopen));
    }

    @FXML
    private void onAddRelationship() {
        if (!busy && customerNumber != null) {
            openEditor(null);
        }
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
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN),
                this::onAddRelationship);
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
        alert.setHeaderText("Relationships");
        alert.setContentText("""
                Ctrl+N     Add a relationship
                Esc        Back to the customer
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
