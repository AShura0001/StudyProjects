package lavs.ui.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import lavs.ui.LavsApp;
import lavs.ui.Navigator;
import lavs.ui.component.CustomerRows;
import lavs.ui.stub.StubCustomerService;

import java.util.List;

/** Full directory: every registered customer, ten per page. */
public class FullDirectoryController {

    private static final int PAGE_SIZE = 10;

    @FXML private BorderPane root;
    @FXML private VBox tableBox;
    @FXML private Label countLabel;
    @FXML private Label pageLabel;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private Label noticeLabel;

    private final StubCustomerService customerService = new StubCustomerService(250);
    private int page;
    private int total;
    private boolean loading;

    @FXML
    private void initialize() {
        setShown(noticeLabel, false);
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerAccelerators(scene);
                Platform.runLater(root::requestFocus);
            }
        });
    }

    /** Shows the given zero-based page; called by whichever screen opens the directory. */
    public void open(int pageIndex) {
        loadPage(pageIndex);
    }

    /** Each accelerator is a second route to a control that is already on screen. */
    private void registerAccelerators(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::onBack);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN), this::onLogout);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.PAGE_DOWN), this::onNext);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.PAGE_UP), this::onPrevious);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), this::showShortcuts);
    }

    private void loadPage(int target) {
        if (loading) {
            return;
        }
        loading = true;
        updatePager();

        Task<List<StubCustomerService.Match>> task = new Task<>() {
            @Override
            protected List<StubCustomerService.Match> call() {
                total = customerService.count();
                return customerService.page(target, PAGE_SIZE);
            }
        };

        task.setOnSucceeded(e -> {
            page = target;
            tableBox.getChildren().setAll(CustomerRows.header());
            for (StubCustomerService.Match m : task.getValue()) {
                tableBox.getChildren().add(CustomerRows.customer(m.name(), m.customerNumber(),
                        () -> openDetails(m.customerNumber())));
            }
            loading = false;
            countLabel.setText(total + " customers registered");
            updatePager();
        });
        task.setOnFailed(e -> {
            loading = false;
            showNotice("Could not reach the mainframe. Try again.");
            updatePager();
        });

        Thread worker = new Thread(task, "directory-page");
        worker.setDaemon(true);
        worker.start();
    }

    private void openDetails(String customerNumber) {
        int returnPage = page;
        Navigator.<CustomerDetailsController>show("customerdetails", c -> c.open(customerNumber,
                () -> Navigator.<FullDirectoryController>show("fulldirectory", d -> d.open(returnPage))));
    }

    private int pageCount() {
        return Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void updatePager() {
        pageLabel.setText("Page " + (page + 1) + " out of " + pageCount());
        previousButton.setDisable(loading || page == 0);
        nextButton.setDisable(loading || page + 1 >= pageCount());
    }

    @FXML
    private void onPrevious() {
        if (!previousButton.isDisabled()) {
            loadPage(page - 1);
        }
    }

    @FXML
    private void onNext() {
        if (!nextButton.isDisabled()) {
            loadPage(page + 1);
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
        alert.setHeaderText("Full directory");
        alert.setContentText("""
                Page Down  Next page
                Page Up    Previous page
                Esc        Back to search
                Ctrl+L     Log out
                F1         This list""");
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
