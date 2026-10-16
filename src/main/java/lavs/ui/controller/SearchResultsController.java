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
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import lavs.ui.Navigator;
import lavs.ui.stub.StubCustomerService;

import java.util.List;
import java.util.Locale;

/** Successful lookup: the search bar kept at the top and a paged results table below. */
public class SearchResultsController {

    private static final int PAGE_SIZE = 3;

    @FXML private BorderPane root;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private ProgressIndicator progress;
    @FXML private VBox tableBox;
    @FXML private Label countLabel;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private Label noticeLabel;

    private final StubCustomerService customerService = new StubCustomerService();
    private List<StubCustomerService.Match> matches = List.of();
    private int page;

    @FXML
    private void initialize() {
        searchField.setTextFormatter(new TextFormatter<String>(change -> {
            change.setText(change.getText().toUpperCase(Locale.ROOT));
            return change;
        }));
        setShown(noticeLabel, false);

        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerAccelerators(scene);
                Platform.runLater(root::requestFocus);
            }
        });
        render();
    }

    /** Called by the previous screen before this one is displayed. */
    public void show(String term, List<StubCustomerService.Match> results) {
        searchField.setText(term);
        matches = results;
        page = 0;
        render();
    }

    /** Each accelerator is a second route to a control that is already on screen. */
    private void registerAccelerators(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), () -> {
            searchField.requestFocus();
            searchField.selectAll();
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::onBack);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN), this::onLogout);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.PAGE_DOWN), this::onNext);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.PAGE_UP), this::onPrevious);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), this::showShortcuts);
    }

    private void render() {
        int total = matches.size();
        countLabel.setText(total + (total == 1 ? " person" : " people") + " found with similar names");

        tableBox.getChildren().setAll(buildRow("Name", "Customer ID", true));
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);
        for (StubCustomerService.Match m : matches.subList(from, to)) {
            tableBox.getChildren().add(buildRow(m.name(), m.customerNumber(), false));
        }

        previousButton.setDisable(page == 0);
        nextButton.setDisable(to >= total);
    }

    private GridPane buildRow(String name, String id, boolean header) {
        GridPane row = new GridPane();
        row.getStyleClass().add(header ? "table-header-row" : "table-row");
        row.setPadding(new Insets(0, 14, 0, 14));
        row.setMinHeight(header ? 30 : 38);
        row.setPrefHeight(header ? 30 : 38);
        row.setAlignment(Pos.CENTER_LEFT);

        for (double w : new double[] {42, 24, 34}) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(w);
            row.getColumnConstraints().add(cc);
        }

        Label nameLabel = new Label(name);
        Label idLabel = new Label(id);
        nameLabel.getStyleClass().add(header ? "table-header-cell" : "table-cell");
        idLabel.getStyleClass().add(header ? "table-header-cell" : "table-cell-muted");
        row.add(nameLabel, 0, 0);
        row.add(idLabel, 1, 0);

        if (header) {
            Label action = new Label("Action");
            action.getStyleClass().add("table-header-cell");
            row.add(action, 2, 0);
        } else {
            Hyperlink details = new Hyperlink("Click here for more details");
            details.getStyleClass().add("link-details");
            details.setTranslateY(-3);
            details.setOnAction(e -> showNotice("The customer details screen has not been built yet."));
            row.add(details, 2, 0);
        }
        return row;
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
            List<StubCustomerService.Match> found = task.getValue();
            if (found.isEmpty()) {
                Navigator.<CustomerSearchController>show("customersearch", c -> c.showNotFoundFor(term));
            } else {
                matches = found;
                page = 0;
                showNotice("");
                render();
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
    private void onPrevious() {
        if (page > 0) {
            page--;
            render();
        }
    }

    @FXML
    private void onNext() {
        if ((page + 1) * PAGE_SIZE < matches.size()) {
            page++;
            render();
        }
    }

    @FXML
    private void onBack() {
        Navigator.show("customersearch");
    }

    @FXML
    private void onLogout() {
        Navigator.show("loggedout");
    }

    @FXML
    private void onRegister() {
        showNotice("Customer registration has not been built yet.");
    }

    private void showNotice(String text) {
        noticeLabel.setText(text);
        setShown(noticeLabel, !text.isEmpty());
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
        alert.setHeaderText("Search results");
        alert.setContentText("""
                Enter      Search again
                Ctrl+F     Focus the search bar
                Page Down  Next page
                Page Up    Previous page
                Esc        Back to search
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
