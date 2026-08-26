package team6;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
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

    private static final long FRAME_INTERVAL_NANOS = 33_000_000; // ~30fps

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
     * @param monitor the search's progress channel, supplying everything the window needs
     */
    public static void launch(SearchMonitor monitor) {
        // Platform.startup throws if the toolkit is already running.
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }

        Platform.startup(() -> showStage(monitor));
    }

    /**
     * Builds and shows the visualisation window, on the JavaFX application thread.
     *
     * @param monitor the search's progress channel, supplying everything the window needs
     */
    private static void showStage(SearchMonitor monitor) {
        Platform.setImplicitExit(false);

        GanttChart ganttChart = new GanttChart(monitor);
        StatsPanel statsPanel = new StatsPanel(monitor);

        BorderPane root = new BorderPane();
        root.setCenter(ganttChart);
        root.setRight(statsPanel);

        Stage stage = new Stage();
        stage.setTitle("Task Scheduler - Visualisation");
        stage.setScene(new Scene(root, 1100, 650));

        AnimationTimer timer = new AnimationTimer() {
            private long lastRepaint = 0;
            private SearchMonitor.Snapshot lastDrawn = null;

            @Override
            public void handle(long now) {
                if (now - lastRepaint < FRAME_INTERVAL_NANOS) {
                    return;
                }
                lastRepaint = now;

                statsPanel.refresh();

                SearchMonitor.Snapshot current = monitor.bestSchedule();
                if (current != lastDrawn) {
                    ganttChart.draw(current);
                    lastDrawn = current;
                }

                if (monitor.isFinished() && lastDrawn == monitor.bestSchedule()) {
                    stop();
                }
            }
        };
        timer.start();

        // onHidden covers every way the window can go away, unlike setOnCloseRequest,
        // which fires before the close and can be vetoed by another handler.
        stage.setOnHidden(event -> {
            timer.stop();
            CLOSED.countDown();
        });
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
