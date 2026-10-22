package lavs.ui.component;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import lavs.business.service.CustomerService;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Deactivating and reactivating a customer, the same on every screen that offers it.
 * The mainframe cannot delete a customer, only mark them Inactive or Active, so that
 * is all this does.
 */
public final class CustomerStatus {

    /** What has to be typed, in capitals, to deactivate a customer. */
    public static final String DELETE_WORD = "DELETE";

    private CustomerStatus() {
    }

    public static boolean isInactive(String status) {
        return "Inactive".equalsIgnoreCase(status == null ? "" : status.trim());
    }

    /** Asks the user to confirm; deactivating needs the word typed, reactivating a plain yes. */
    public static boolean confirm(Window owner, boolean currentlyInactive) {
        return currentlyInactive ? confirmReactivate(owner) : confirmTyped(owner);
    }

    private static boolean confirmReactivate(Window owner) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Reactivate this customer? They will be marked Active on the mainframe again.",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Reactivate customer");
        alert.setHeaderText(null);
        alert.initOwner(owner);
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.YES;
    }

    /** Deactivating needs the word DELETE typed exactly, capitals included. */
    private static boolean confirmTyped(Window owner) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Deactivate customer");
        dialog.initOwner(owner);
        ButtonType confirm = new ButtonType("Deactivate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirm, ButtonType.CANCEL);

        Label message = new Label("This customer will be marked Inactive on the mainframe. The record is kept "
                + "(the mainframe cannot delete customers) and can be reactivated at any time.\n\n"
                + "Type " + DELETE_WORD + " in capitals to confirm.");
        message.setWrapText(true);
        message.setMaxWidth(380);
        TextField typed = new TextField();
        typed.setPromptText(DELETE_WORD);
        dialog.getDialogPane().setContent(new VBox(10, message, typed));

        Node confirmButton = dialog.getDialogPane().lookupButton(confirm);
        confirmButton.setDisable(true);
        typed.textProperty().addListener((obs, old, now) -> confirmButton.setDisable(!DELETE_WORD.equals(now)));
        Platform.runLater(typed::requestFocus);

        Optional<ButtonType> answer = dialog.showAndWait();
        return answer.isPresent() && answer.get() == confirm;
    }

    /**
     * Flips the customer's status off the JavaFX thread, then calls one of the two
     * callbacks back on it: {@code onDone} when the mainframe made the change,
     * {@code onProblem} with a message for the user when it did not.
     */
    public static void flip(CustomerService customers, String customerNumber,
                            Runnable onDone, Consumer<String> onProblem) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return customers.toggleCustomerStatus(customerNumber);
            }
        };
        task.setOnSucceeded(e -> {
            if (Boolean.TRUE.equals(task.getValue())) {
                onDone.run();
            } else {
                onProblem.accept("The mainframe did not change the customer's status.");
            }
        });
        task.setOnFailed(e -> onProblem.accept(
                Format.problem(task.getException(), "Could not reach the mainframe. Try again.")));
        Thread worker = new Thread(task, "customer-status");
        worker.setDaemon(true);
        worker.start();
    }
}
