package team6;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Class that handles graph visualisation.
 *
 * <p>Deliberately does not extend {@link javafx.application.Application}, whose
 * {@code launch} takes over the calling thread and only returns once the last window
 * closes. Starting the toolkit with {@link Platform#startup} instead leaves the
 * scheduler in control of the main thread. Every JavaFX reference in the project is
 * confined to this class, so a headless run never loads one.
 */
public class Visualiser {

    /** Released once the visualisation window has been closed by the user. */
    private static final CountDownLatch CLOSED = new CountDownLatch(1);

    /** Guards against booting the JavaFX toolkit more than once. */
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private Visualiser() {
        // Static utility, not meant to be instantiated.
    }

    /**
     * Boots the JavaFX toolkit and shows the visualisation window, without waiting for it.
     *
     * @param nProcessors number of processors the schedule is being built for
     * @param nWorkers    number of worker threads running the search
     */
    public static void launch(int nProcessors, int nWorkers) {
        // Platform.startup throws if the toolkit is already running.
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }

        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            showStage(nProcessors, nWorkers);
        });
    }

    /**
     * Builds and shows the visualisation window, on the JavaFX application thread.
     *
     * @param nProcessors number of processors the schedule is being built for
     * @param nWorkers    number of worker threads running the search
     */
    private static void showStage(int nProcessors, int nWorkers) {
        Label placeholder = new Label(
                "Visualisation placeholder\n\n"
                        + "Processors: " + nProcessors + "\n"
                        + "Worker threads: " + nWorkers + "\n\n"
                        + "Close this window to end the program.");
        placeholder.setStyle("-fx-font-size: 14px;");

        StackPane root = new StackPane(placeholder);
        StackPane.setAlignment(placeholder, Pos.CENTER);

        Stage stage = new Stage();
        stage.setTitle("Task Scheduler - Visualisation");
        stage.setScene(new Scene(root, 900, 600));

        // onHidden covers every way the window can go away, unlike setOnCloseRequest,
        // which fires before the close and can be vetoed by another handler.
        stage.setOnHidden(event -> CLOSED.countDown());
        stage.show();
    }

    /**
     * Blocks until the user closes the visualisation window, or returns immediately if
     * the visualisation was never started.
     */
    public static void awaitClose() {
        if (!STARTED.get()) {
            return;
        }
        try {
            CLOSED.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            Platform.exit();
        }
    }
}
