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
import javafx.scene.layout.VBox;

import lavs.ui.Navigator;
import lavs.ui.LavsApp;
import lavs.ui.component.CustomerRows;
import lavs.ui.component.CustomerStatus;
import lavs.business.model.Customer;
import lavs.business.service.CustomerService;
import lavs.ui.component.Format;

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
    @FXML private Label pageLabel;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private Label noticeLabel;

    /** Searches read at most this many pages of 100 matches. */
    static final int MAX_SEARCH_PAGES = 5;

    private final CustomerService customerService = LavsApp.context().customers();
    private List<Customer> matches = List.of();
    private int page;
    private boolean statusBusy;

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
    public void show(String term, List<Customer> results) {
        show(term, results, 0);
    }

    public void show(String term, List<Customer> results, int pageIndex) {
        searchField.setText(term);
        matches = results;
        page = pageIndex;
        render();
    }

    private void openDetails(String customerNumber) {
        String term = searchField.getText();
        List<Customer> shown = matches;
        int returnPage = page;
        Navigator.<CustomerDetailsController>show("customerdetails", c -> c.open(customerNumber,
                () -> Navigator.<SearchResultsController>show("searchresults", r -> r.show(term, shown, returnPage))));
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
        int pageCount = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        pageLabel.setText("Page " + (page + 1) + " out of " + pageCount);

        tableBox.getChildren().setAll(CustomerRows.header());
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);
        for (Customer m : matches.subList(from, to)) {
            tableBox.getChildren().add(CustomerRows.customer(Format.name(m), Format.text(m.get_number()),
                    m.get_status(), () -> openDetails(m.get_number()), () -> changeStatus(m)));
        }

        previousButton.setDisable(page == 0);
        nextButton.setDisable(to >= total);
    }

    private void changeStatus(Customer customer) {
        if (progress.isVisible() || statusBusy) {
            return;
        }
        boolean inactive = CustomerStatus.isInactive(customer.get_status());
        if (!CustomerStatus.confirm(root.getScene().getWindow(), inactive)) {
            return;
        }
        statusBusy = true;
        showNotice("");
        CustomerStatus.flip(customerService, customer.get_number(), () -> {
            statusBusy = false;
            customer.set_status(inactive ? "Active" : "Inactive");
            render();
            showNotice(Format.name(customer) + (inactive ? " reactivated." : " deactivated."));
        }, problem -> {
            statusBusy = false;
            showNotice(problem);
        });
    }

    @FXML
    private void onSearch() {
        String term = searchField.getText().trim();
        if (term.isEmpty()) {
            return;
        }

        Task<List<Customer>> task = new Task<>() {
            @Override
            protected List<Customer> call() throws Exception {
                return customerService.searchAll(term, MAX_SEARCH_PAGES);
            }
        };

        task.setOnSucceeded(e -> {
            setBusy(false);
            List<Customer> found = task.getValue();
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
            showNotice(Format.problem(task.getException(), "Could not reach the mainframe. Try again."));
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
        LavsApp.context().logout();
        Navigator.show("loggedout");
    }

    @FXML
    private void onRegister() {
        Navigator.show("registercustomer");
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
