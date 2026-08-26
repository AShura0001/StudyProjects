package team6;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.scene.Scene;
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

    /** Set once the window is showing, so showFinalSchedule() can find the chart and monitor. */
    private static volatile GanttChart chart;
    private static volatile SearchMonitor monitor;

    private Visualiser() {
        // Static utility, not meant to be instantiated.
    }

    /**
     * Boots the JavaFX toolkit and shows the visualisation window, without waiting for it.
     *
     * @param monitor the search's progress channel, supplying everything the window needs
     */
    public static void launch(SearchMonitor searchMonitor) {
        // Platform.startup throws if the toolkit is already running.
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        monitor = searchMonitor;

        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            showStage(searchMonitor);
        });
    }

    /**
     * Builds and shows the visualisation window, on the JavaFX application thread.
     *
     * @param monitor the search's progress channel, supplying everything the window needs
     */
    private static void showStage(SearchMonitor monitor) {
        GanttChart ganttChart = new GanttChart(monitor);
        chart = ganttChart;

        Stage stage = new Stage();
        stage.setTitle("Task Scheduler - Visualisation");
        stage.setScene(new Scene(ganttChart, 900, 600));

        // onHidden covers every way the window can go away, unlike setOnCloseRequest,
        // which fires before the close and can be vetoed by another handler.
        stage.setOnHidden(event -> CLOSED.countDown());
        stage.show();
    }

    /**
     * Draws the launched monitor's best schedule so far. Safe to call from any thread; the
     * draw itself is dispatched onto the JavaFX application thread.
     */
    public static void showFinalSchedule() {
        if (!STARTED.get()) {
            return;
        }
        Platform.runLater(() -> {
            GanttChart ganttChart = chart;
            if (ganttChart != null) {
                ganttChart.draw(monitor.bestSchedule());
            }
        });
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
