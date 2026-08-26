package team6;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
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
        WorkerPanel workerPanel = new WorkerPanel(monitor);

        VBox rightColumn = new VBox(statsPanel, workerPanel);
        ScrollPane rightScroll = new ScrollPane(rightColumn);
        rightScroll.setFitToWidth(true);

        BorderPane root = new BorderPane();
        root.setCenter(ganttChart);
        root.setRight(rightScroll);

        Stage stage = new Stage();
        stage.setTitle("Task Scheduler - Visualisation");
        stage.setScene(new Scene(root, 1100, 650));

        Timeline timeline = new Timeline();

        AnimationTimer timer = new AnimationTimer() {
            private long lastRepaint = 0;
            private SearchMonitor.Snapshot lastDrawnSnapshot = null;
            private Set<Integer> lastDrawnHighlight = Collections.emptySet();

            @Override
            public void handle(long now) {
                if (now - lastRepaint < FRAME_INTERVAL_NANOS) {
                    return;
                }
                lastRepaint = now;

                Timeline.Frame frame = Timeline.capture(monitor);
                render(frame);
                timeline.offer(frame);

                if (monitor.isFinished() && lastDrawnSnapshot == monitor.bestSchedule()) {
                    // markFinished() can land between the render above and this check, so
                    // render once more here rather than risk stopping on a stale "Searching".
                    Timeline.Frame finalFrame = Timeline.capture(monitor);
                    render(finalFrame);
                    timeline.offer(finalFrame);
                    stop();
                }
            }

            private void render(Timeline.Frame frame) {
                if (frame.schedule() != lastDrawnSnapshot || !frame.highlighted().equals(lastDrawnHighlight)) {
                    ganttChart.draw(frame);
                    lastDrawnSnapshot = frame.schedule();
                    lastDrawnHighlight = frame.highlighted();
                }
                statsPanel.render(frame);
                workerPanel.render(frame);
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
