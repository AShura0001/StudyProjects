package lavs.ui.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;

import lavs.ui.Navigator;
import lavs.ui.stub.StubCustomerService;

import java.util.List;
import java.util.Locale;

/**
 * Customer Search. One screen with two states so far: the idle search bar and
 * the "customer does not exist" state. The results state is a separate frame
 * and is built next.
 */
public class CustomerSearchController {

    @FXML private BorderPane root;
    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private ProgressIndicator progress;
    @FXML private Node idleInfo;
    @FXML private Node notFoundInfo;
    @FXML private Label noticeLabel;

    private final StubCustomerService customerService = new StubCustomerService();

    @FXML
    private void initialize() {
        // Names and customer numbers are shown in capitals in the design.
        searchField.setTextFormatter(new TextFormatter<String>(change -> {
            change.setText(change.getText().toUpperCase(Locale.ROOT));
            return change;
        }));

        searchField.textProperty().addListener((obs, old, now) -> showIdle());
        showIdle();

        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerAccelerators(scene);
                Platform.runLater(searchField::requestFocus);
            }
        });
    }

    /** Each accelerator is a second route to a control that is already on screen. */
    private void registerAccelerators(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), () -> {
            searchField.requestFocus();
            searchField.selectAll();
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::onBack);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN), this::onLogout);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), this::showShortcuts);
    }

    @FXML
    private void onSearch() {
        String term = searchField.getText().trim();
        if (term.isEmpty()) {
            return;
        }

        Task<List<StubCustomerService.Match>> task = new Task<>() {
            @Override
            protected List<StubCustomerService.Match> call() {
                return customerService.search(term);
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false);
            List<StubCustomerService.Match> matches = task.getValue();
            if (matches.isEmpty()) {
                showNotFound();
            } else {
                Navigator.<SearchResultsController>show("searchresults", c -> c.show(term, matches));
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showNotice("Could not reach the mainframe. Try again.");
        });

        setBusy(true);
        Thread worker = new Thread(task, "customer-search");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void onBack() {
        searchField.clear();
        showIdle();
        searchField.requestFocus();
    }

    @FXML
    private void onLogout() {
        Navigator.show("loggedout");
    }

    @FXML
    private void onFullDirectory() {
        Navigator.show("fulldirectory");
    }

    @FXML
    private void onRegister() {
        showNotice("Customer registration has not been built yet.");
    }

    private void showNotice(String text) {
        noticeLabel.setText(text);
        setShown(noticeLabel, true);
    }

    /** Used by the results screen when a follow-up search finds nobody. */
    public void showNotFoundFor(String term) {
        searchField.setText(term);
        showNotFound();
    }

    private void showIdle() {
        setShown(idleInfo, true);
        setShown(notFoundInfo, false);
        setShown(searchButton, true);
        setShown(backButton, false);
        noticeLabel.setText("");
        setShown(noticeLabel, false);
    }

    private void showNotFound() {
        setShown(idleInfo, false);
        setShown(notFoundInfo, true);
        setShown(searchButton, false);
        setShown(backButton, true);
        searchField.deselect();
        searchField.positionCaret(searchField.getLength());
    }

    private void setBusy(boolean busy) {
        setShown(searchButton, !busy);
        setShown(progress, busy);
        searchField.setDisable(busy);
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private void showShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard shortcuts");
        alert.setHeaderText("Customer Search");
        alert.setContentText("""
                Enter      Search
                Ctrl+F     Focus the search bar
                Esc        Clear and go back
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
