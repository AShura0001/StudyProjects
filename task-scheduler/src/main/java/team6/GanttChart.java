package team6;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Draws the best schedule found so far as a Gantt chart. Holds a single {@link Canvas}
 * child and repaints it whenever a new snapshot arrives or the pane is resized.
 */
final class GanttChart extends Pane {

    private static final double GUTTER = 48;
    private static final double AXIS_HEIGHT = 28;
    private static final double PADDING = 12;
    private static final double MIN_ROW_HEIGHT = 18;
    private static final double MAX_ROW_HEIGHT = 56;

    private static final Color COLOR_BACKGROUND = Color.web("#FFFFFF");
    private static final Color COLOR_GRIDLINES = Color.web("#E4E4E7");
    private static final Color COLOR_ROW_LABEL = Color.web("#27272A");
    private static final Color COLOR_AXIS_TICK = Color.web("#52525B");
    private static final Color COLOR_BAR_FILL = Color.web("#356094");
    private static final Color COLOR_BAR_BORDER = Color.web("#24486F");
    private static final Color COLOR_BAR_LABEL = Color.web("#FFFFFF");
    private static final Color COLOR_EMPTY_ROW_BAND = Color.web("#FAFAFA");

    private final SearchMonitor monitor;
    private final Canvas canvas;
    private volatile SearchMonitor.Snapshot snapshot;

    GanttChart(SearchMonitor monitor) {
        this.monitor = monitor;
        this.canvas = new Canvas();
        getChildren().add(canvas);
    }

    /**
     * Stores the given snapshot and repaints. Must be called on the JavaFX application
     * thread.
     */
    void draw(SearchMonitor.Snapshot snapshot) {
        this.snapshot = snapshot;
        repaint();
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        canvas.setWidth(width);
        canvas.setHeight(height);
        repaint();
    }

    private void repaint() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(COLOR_BACKGROUND);
        gc.fillRect(0, 0, width, height);

        int processorCount = monitor.processorCount();
        if (processorCount <= 0 || width <= 0 || height <= 0) {
            return;
        }

        SearchMonitor.Snapshot current = this.snapshot;

        double titleHeight = 24;
        gc.setFont(Font.font(null, FontWeight.BOLD, 14));
        gc.setFill(COLOR_ROW_LABEL);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        String title = current == null
                ? "Best schedule - searching"
                : "Best schedule - makespan " + current.makespan();
        gc.fillText(title, PADDING, PADDING);

        double top = PADDING + titleHeight;
        double plotHeight = height - top - AXIS_HEIGHT - PADDING;
        double plotWidth = width - GUTTER - PADDING;
        if (plotHeight <= 0 || plotWidth <= 0) {
            return;
        }

        double rowHeight = plotHeight / processorCount;
        rowHeight = Math.max(MIN_ROW_HEIGHT, Math.min(MAX_ROW_HEIGHT, rowHeight));

        // Row label bands, drawn first so gridlines and bars sit on top.
        gc.setFill(COLOR_EMPTY_ROW_BAND);
        gc.fillRect(GUTTER, top, plotWidth, rowHeight * processorCount);

        int makespan = current == null ? 1 : Math.max(1, current.makespan());
        double xScale = plotWidth / makespan;

        double tickStep = niceTickStep(makespan, 8);
        gc.setStroke(COLOR_GRIDLINES);
        gc.setLineWidth(1);
        for (double t = 0; t <= makespan; t += tickStep) {
            double x = barX((int) Math.round(t), xScale, GUTTER);
            gc.strokeLine(x, top, x, top + rowHeight * processorCount);
        }

        // Row labels P1..Pn.
        gc.setFont(Font.font(12));
        gc.setFill(COLOR_ROW_LABEL);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setTextBaseline(VPos.CENTER);
        for (int p = 0; p < processorCount; p++) {
            double y = top + p * rowHeight + rowHeight / 2;
            gc.fillText("P" + (p + 1), GUTTER - 6, y);
        }

        // Axis ticks.
        gc.setFont(Font.font(11));
        gc.setFill(COLOR_AXIS_TICK);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.TOP);
        double axisY = top + rowHeight * processorCount + 6;
        for (double t = 0; t <= makespan; t += tickStep) {
            double x = barX((int) Math.round(t), xScale, GUTTER);
            gc.fillText(Integer.toString((int) Math.round(t)), x, axisY);
        }

        if (current == null) {
            gc.setFont(Font.font(null, FontWeight.NORMAL, 16));
            gc.setFill(COLOR_ROW_LABEL);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText("Searching...", GUTTER + plotWidth / 2, top + rowHeight * processorCount / 2);
            return;
        }

        int[] startTimes = current.startTimes();
        int[] processors = current.processors();
        for (int i = 0; i < startTimes.length; i++) {
            drawBar(gc, i, startTimes[i], processors[i], xScale, top, rowHeight);
        }
    }

    private void drawBar(GraphicsContext gc, int taskIndex, int startTime, int processorNumber,
            double xScale, double top, double rowHeight) {
        int row = rowIndex(processorNumber);
        if (row < 0 || row >= monitor.processorCount()) {
            return;
        }
        int weight = monitor.taskWeight(taskIndex);
        double x = barX(startTime, xScale, GUTTER);
        double w = barWidth(weight, xScale);
        double y = top + row * rowHeight + 1;
        double h = rowHeight - 2;
        double radius = Math.min(4, Math.min(h / 2, w / 2));

        gc.setFill(COLOR_BAR_FILL);
        gc.fillRoundRect(x, y, w, h, radius * 2, radius * 2);
        gc.setStroke(COLOR_BAR_BORDER);
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, w, h, radius * 2, radius * 2);

        String name = monitor.taskName(taskIndex);
        Text measure = new Text(name);
        measure.setFont(Font.font(null, FontWeight.BOLD, 11));
        double textWidth = measure.getLayoutBounds().getWidth();
        if (textWidth + 8 <= w) {
            gc.setFont(Font.font(null, FontWeight.BOLD, 11));
            gc.setFill(COLOR_BAR_LABEL);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(name, x + w / 2, y + h / 2);
        }
    }

    /**
     * Returns a "nice" tick step - 1, 2, or 5 scaled by a power of ten - such that
     * roughly targetTickCount ticks span the makespan. Never returns 0.
     */
    static double niceTickStep(int makespan, int targetTickCount) {
        int span = Math.max(1, makespan);
        int ticks = Math.max(1, targetTickCount);
        double roughStep = (double) span / ticks;
        double magnitude = Math.pow(10, Math.floor(Math.log10(roughStep)));
        double normalized = roughStep / magnitude;

        double niceNormalized;
        if (normalized <= 1) {
            niceNormalized = 1;
        } else if (normalized <= 2) {
            niceNormalized = 2;
        } else if (normalized <= 5) {
            niceNormalized = 5;
        } else {
            niceNormalized = 10;
        }
        double step = niceNormalized * magnitude;
        return step <= 0 ? 1 : step;
    }

    /** X coordinate of a bar/tick starting at startTime, given the current scale and gutter. */
    static double barX(int startTime, double xScale, double gutter) {
        return gutter + startTime * xScale;
    }

    /** Converts a 1-based processor number (as stored in Snapshot.processors()) to a 0-based row. */
    static int rowIndex(int processorNumber) {
        return processorNumber - 1;
    }

    /** Pixel width of a bar of the given weight, given the current scale. Never less than 2. */
    static double barWidth(int weight, double xScale) {
        return Math.max(2, weight * xScale);
    }
}
