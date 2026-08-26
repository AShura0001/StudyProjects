package team6;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
        PlaybackBar playbackBar = new PlaybackBar();

        VBox rightColumn = new VBox(statsPanel, workerPanel);
        ScrollPane rightScroll = new ScrollPane(rightColumn);
        rightScroll.setFitToWidth(true);

        BorderPane root = new BorderPane();
        root.setCenter(ganttChart);
        root.setRight(rightScroll);
        root.setBottom(playbackBar);

        Stage stage = new Stage();
        stage.setTitle("Task Scheduler - Visualisation");
        Scene scene = new Scene(root, 1100, 650);
        stage.setScene(scene);

        Controller controller = new Controller(monitor, ganttChart, statsPanel, workerPanel, playbackBar);
        scene.setOnKeyPressed(controller::handleKeyPress);
        controller.startLive();

        // onHidden covers every way the window can go away, unlike setOnCloseRequest,
        // which fires before the close and can be vetoed by another handler.
        stage.setOnHidden(event -> {
            controller.stop();
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

    /**
     * Drives the window through its two modes: LIVE, which polls the monitor and records into
     * a {@link Timeline} as the search runs, and REPLAY, which plays that timeline back on its
     * own clock once the search is done. Everything mutable lives here rather than in the
     * timer itself, so the transport buttons, slider and keyboard shortcuts - all wired in the
     * constructor - can reach it too.
     */
    private static final class Controller {

        private final SearchMonitor monitor;
        private final Timeline timeline = new Timeline();
        private final GanttChart ganttChart;
        private final StatsPanel statsPanel;
        private final WorkerPanel workerPanel;
        private final PlaybackBar bar;
        private final AnimationTimer timer;

        private long lastRepaintNanos = 0;

        // LIVE-mode redraw-only-when-changed bookkeeping for the Gantt chart.
        private SearchMonitor.Snapshot lastDrawnSnapshot = null;
        private Set<Integer> lastDrawnHighlight = Collections.emptySet();

        private boolean replaying = false;
        private int currentIndex = -1;

        // REPLAY-mode anchor: the wall-clock instant and the frame elapsed time it corresponds
        // to, so playback tracks real elapsed time rather than ticks of the UI's own clock.
        private long anchorWallNanos;
        private long anchorFrameMillis;

        Controller(SearchMonitor monitor, GanttChart ganttChart, StatsPanel statsPanel,
                WorkerPanel workerPanel, PlaybackBar bar) {
            this.monitor = monitor;
            this.ganttChart = ganttChart;
            this.statsPanel = statsPanel;
            this.workerPanel = workerPanel;
            this.bar = bar;

            bar.prevImprovementButton.setOnAction(e -> jumpToImprovement(false));
            bar.nextImprovementButton.setOnAction(e -> jumpToImprovement(true));
            bar.playPauseButton.setOnAction(e -> togglePlayPause());
            bar.speedChoice.setOnAction(e -> {
                if (replaying) {
                    anchor();
                }
            });
            bar.slider.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (bar.isSuppressingSliderEvent()) {
                    return;
                }
                scrubTo((int) Math.round(newValue.doubleValue()));
            });

            this.timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    Controller.this.handle(now);
                }
            };
        }

        void startLive() {
            timer.start();
        }

        void stop() {
            timer.stop();
        }

        private void handle(long now) {
            if (now - lastRepaintNanos < FRAME_INTERVAL_NANOS) {
                return;
            }
            lastRepaintNanos = now;

            if (replaying) {
                tickReplay(now);
            } else {
                tickLive();
            }
        }

        // ---- LIVE mode ----

        private void tickLive() {
            Timeline.Frame frame = Timeline.capture(monitor);
            renderLive(frame);
            timeline.offer(frame);

            if (monitor.isFinished() && lastDrawnSnapshot == monitor.bestSchedule()) {
                // markFinished() can land between the render above and this check, so
                // render once more here rather than risk stopping on a stale "Searching".
                Timeline.Frame finalFrame = Timeline.capture(monitor);
                renderLive(finalFrame);
                timeline.offer(finalFrame);
                finishLive();
            }
        }

        private void renderLive(Timeline.Frame frame) {
            if (frame.schedule() != lastDrawnSnapshot || !frame.highlighted().equals(lastDrawnHighlight)) {
                ganttChart.draw(frame);
                lastDrawnSnapshot = frame.schedule();
                lastDrawnHighlight = frame.highlighted();
            }
            statsPanel.render(frame);
            workerPanel.render(frame);
        }

        private void finishLive() {
            timer.stop();
            currentIndex = timeline.size() - 1;
            bar.reveal(timeline);
            bar.showIndex(currentIndex);
        }

        // ---- REPLAY mode ----

        private void tickReplay(long nowNanos) {
            long virtualMillis = anchorFrameMillis
                    + (long) ((nowNanos - anchorWallNanos) / 1_000_000.0 * currentSpeed());
            int newIndex = timeline.frameAtTime(virtualMillis, currentIndex);
            if (newIndex != currentIndex) {
                currentIndex = newIndex;
                showFrame(currentIndex);
            }

            if (currentIndex >= timeline.size() - 1) {
                pause();
                // Same as togglePlayPause's pause path: no further frame change follows here,
                // so Status needs its own explicit refresh back off "Replay".
                statsPanel.render(timeline.frame(currentIndex));
            }
        }

        private double currentSpeed() {
            Double speed = bar.speedChoice.getValue();
            return speed == null ? 1.0 : speed;
        }

        private void play() {
            if (timeline.size() < 2) {
                return;
            }
            // Flip into replay mode before any render below, or a reset-to-start render would
            // go out with Status still reading "Complete" for one frame.
            replaying = true;
            statsPanel.setReplayMode(true);
            if (currentIndex >= timeline.size() - 1) {
                currentIndex = 0; // replay again from the start once it's already reached the end
                showFrame(currentIndex);
            }
            anchor();
            bar.setPlaying(true);
            timer.start();
        }

        private void pause() {
            replaying = false;
            timer.stop();
            statsPanel.setReplayMode(false);
            bar.setPlaying(false);
        }

        private void anchor() {
            anchorWallNanos = System.nanoTime();
            anchorFrameMillis = timeline.frame(currentIndex).elapsedMillis();
        }

        private void togglePlayPause() {
            if (replaying) {
                pause();
                // No accompanying frame change here, unlike every other path that calls pause()
                // mid-navigation, so Status needs its own explicit refresh back off "Replay".
                statsPanel.render(timeline.frame(currentIndex));
            } else {
                play();
            }
        }

        private void showFrame(int index) {
            Timeline.Frame frame = timeline.frame(index);
            ganttChart.draw(frame);
            statsPanel.render(frame);
            workerPanel.render(frame);
            bar.showIndex(index);
        }

        private void stepFrame(int delta) {
            if (timeline.size() == 0) {
                return;
            }
            if (replaying) {
                pause();
            }
            currentIndex = clamp(currentIndex + delta, 0, timeline.size() - 1);
            showFrame(currentIndex);
        }

        private void jumpToImprovement(boolean forward) {
            if (timeline.size() == 0) {
                return;
            }
            int target = forward ? timeline.nextImprovement(currentIndex) : timeline.prevImprovement(currentIndex);
            if (target == -1) {
                return;
            }
            if (replaying) {
                pause();
            }
            currentIndex = target;
            showFrame(currentIndex);
        }

        private void scrubTo(int index) {
            if (replaying) {
                pause();
            }
            currentIndex = clamp(index, 0, Math.max(0, timeline.size() - 1));
            showFrame(currentIndex);
        }

        private void handleKeyPress(KeyEvent event) {
            if (!bar.isVisible()) {
                return;
            }
            KeyCode code = event.getCode();
            if (code == KeyCode.SPACE) {
                togglePlayPause();
            } else if (code == KeyCode.LEFT) {
                if (event.isShiftDown()) {
                    jumpToImprovement(false);
                } else {
                    stepFrame(-1);
                }
            } else if (code == KeyCode.RIGHT) {
                if (event.isShiftDown()) {
                    jumpToImprovement(true);
                } else {
                    stepFrame(1);
                }
            }
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
