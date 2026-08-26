package team6;

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Right-hand statistics panel: six labelled rows rendered from a {@link Timeline.Frame} on
 * demand by the caller (an {@link javafx.animation.AnimationTimer} in {@link Visualiser}).
 */
final class StatsPanel extends VBox {

    private static final double WIDTH = 220;
    private static final double PADDING = 16;
    private static final double ROW_SPACING = 14;

    private static final Color COLOR_BACKGROUND = Color.web("#FAFAFA");
    private static final Color COLOR_DIVIDER = Color.web("#E4E4E7");
    private static final Color COLOR_ROW_LABEL = Color.web("#52525B");
    private static final Color COLOR_VALUE = Color.web("#18181B");
    private static final Color COLOR_STATUS_SEARCHING = Color.web("#356094");
    private static final Color COLOR_STATUS_COMPLETE = Color.web("#15803D");
    private static final Color COLOR_STATUS_REPLAY = Color.web("#C2410C");

    private final SearchMonitor monitor;

    private final Text status = new Text();
    private final Text elapsed = new Text();
    private final Text states = new Text();
    private final Text best = new Text();
    private final Text depth = new Text();
    private final Text workers = new Text();

    private boolean replayMode = false;

    StatsPanel(SearchMonitor monitor) {
        this.monitor = monitor;

        setPrefWidth(WIDTH);
        setPadding(new Insets(PADDING));
        setSpacing(ROW_SPACING);
        setBackground(new Background(new BackgroundFill(COLOR_BACKGROUND, CornerRadii.EMPTY, Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(COLOR_DIVIDER, COLOR_DIVIDER, COLOR_DIVIDER, COLOR_DIVIDER,
                BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(0, 0, 0, 1), Insets.EMPTY)));

        getChildren().addAll(
                row("Status", status),
                row("Elapsed", elapsed),
                row("States", states),
                row("Best", best),
                row("Depth", depth),
                row("Workers", workers));

        render(Timeline.capture(monitor));
    }

    private VBox row(String label, Text value) {
        Text labelText = new Text(label);
        labelText.setFill(COLOR_ROW_LABEL);
        labelText.setFont(Font.font("Monospaced", 11));

        value.setFill(COLOR_VALUE);
        value.setFont(Font.font("Monospaced", 18));

        VBox box = new VBox(2, labelText, value);
        return box;
    }

    /** While true, Status reads "Replay" instead of "Complete", so a recording is never mistaken for a live search. */
    void setReplayMode(boolean replayMode) {
        this.replayMode = replayMode;
    }

    /**
     * Updates the row values from the given frame. isFinished, taskCount and workerCount come
     * from the monitor - they're static/structural, not part of what changes moment to moment.
     * Must be called on the JavaFX application thread.
     */
    void render(Timeline.Frame frame) {
        if (replayMode) {
            status.setText("Replay");
            status.setFill(COLOR_STATUS_REPLAY);
        } else {
            boolean finished = monitor.isFinished();
            status.setText(finished ? "Complete" : "Searching");
            status.setFill(finished ? COLOR_STATUS_COMPLETE : COLOR_STATUS_SEARCHING);
        }

        elapsed.setText(formatElapsed(frame.elapsedMillis()));
        states.setText(formatCount(frame.statesExplored()));

        int bestMakespan = frame.bestMakespan();
        best.setText(bestMakespan < 0 ? "-" : Integer.toString(bestMakespan));

        depth.setText(frame.maxDepth() + " / " + monitor.taskCount());
        workers.setText(frame.activeWorkers() + " / " + monitor.workerCount());
    }

    /** Formats milliseconds as m:ss.t, e.g. 4234 -> "0:04.2". */
    static String formatElapsed(long millis) {
        long totalTenths = millis / 100;
        long minutes = totalTenths / 600;
        long seconds = (totalTenths / 10) % 60;
        long tenths = totalTenths % 10;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds + "." + tenths;
    }

    /** Formats a count with thousands separators, or as N.NM above a million. */
    static String formatCount(long n) {
        if (n >= 1_000_000) {
            return String.format("%.1fM", n / 1_000_000.0);
        }
        return String.format("%,d", n);
    }
}
