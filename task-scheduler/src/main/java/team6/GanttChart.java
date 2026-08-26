package team6;

import java.util.Set;

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
 * child and repaints it whenever a new frame arrives or the pane is resized.
 */
final class GanttChart extends Pane {

    private static final double GUTTER = 48;
    private static final double AXIS_HEIGHT = 28;
    private static final double PADDING = 12;
    private static final double MIN_ROW_HEIGHT = 18;
    private static final double MAX_ROW_HEIGHT = 90;
    private static final double TITLE_LINE_HEIGHT = 22;
    private static final double SUBTITLE_LINE_HEIGHT = 16;
    private static final double TITLE_BLOCK_HEIGHT = TITLE_LINE_HEIGHT + SUBTITLE_LINE_HEIGHT;

    private static final Color COLOR_BACKGROUND = Color.web("#FFFFFF");
    private static final Color COLOR_GRIDLINES = Color.web("#E4E4E7");
    private static final Color COLOR_TITLE_INK = Color.web("#18181B");
    private static final Color COLOR_SUBTITLE_INK = Color.web("#52525B");
    private static final Color COLOR_AXIS_TICK = Color.web("#52525B");
    private static final Color COLOR_BAR_LABEL_INK = Color.web("#18181B");
    private static final Color COLOR_EMPTY_ROW_BAND = Color.web("#F4F4F5");
    private static final Color COLOR_HIGHLIGHT_RING = Color.web("#C2410C");

    /**
     * A validated categorical hue sequence - adjacent pairs stay separable under colour-vision
     * deficiency - light enough to tint with dark ink text at 13-15:1 contrast. Indexed by
     * processorNumber - 1; do not reorder or substitute hues, and do not cycle it past index 7
     * (see {@link #colorFor}).
     */
    private static final ProcessorColor[] PROCESSOR_COLORS = {
        new ProcessorColor(Color.web("#D9E7F8"), Color.web("#2A78D6")), // 1 blue
        new ProcessorColor(Color.web("#FBE4DA"), Color.web("#EB6834")), // 2 orange
        new ProcessorColor(Color.web("#D6F1E7"), Color.web("#1BAF7A")), // 3 aqua
        new ProcessorColor(Color.web("#FCEED1"), Color.web("#EDA100")), // 4 yellow
        new ProcessorColor(Color.web("#FBE7EF"), Color.web("#E87BA4")), // 5 magenta
        new ProcessorColor(Color.web("#D1E9D1"), Color.web("#008300")), // 6 green
        new ProcessorColor(Color.web("#DEDCEF"), Color.web("#4A3AA7")), // 7 violet
        new ProcessorColor(Color.web("#FADEDE"), Color.web("#E34948")), // 8 red
    };

    private final SearchMonitor monitor;
    private final Canvas canvas;
    private volatile Timeline.Frame frame;

    GanttChart(SearchMonitor monitor) {
        this.monitor = monitor;
        this.canvas = new Canvas();
        getChildren().add(canvas);
    }

    /**
     * Stores the given frame and repaints. The frame's highlighted task indices are the ones
     * currently being evaluated by any worker, drawn as a ring around their bar. Must be
     * called on the JavaFX application thread.
     */
    void draw(Timeline.Frame frame) {
        this.frame = frame;
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

        Timeline.Frame frame = this.frame;
        SearchMonitor.Snapshot current = frame == null ? null : frame.schedule();
        Set<Integer> highlighted = frame == null ? Set.of() : frame.highlighted();

        double plotWidth = width - GUTTER - PADDING;
        double availableHeight = height - 2 * PADDING;
        double rawPlotHeight = availableHeight - TITLE_BLOCK_HEIGHT - AXIS_HEIGHT;
        if (plotWidth <= 0 || rawPlotHeight <= 0) {
            return;
        }

        double rowHeight = rawPlotHeight / processorCount;
        rowHeight = Math.max(MIN_ROW_HEIGHT, Math.min(MAX_ROW_HEIGHT, rowHeight));

        // If the rows (now capped) leave room to spare, centre the title+rows+axis block
        // vertically instead of pinning it to the top of the pane.
        double contentHeight = TITLE_BLOCK_HEIGHT + rowHeight * processorCount + AXIS_HEIGHT;
        double blockTop = PADDING + Math.max(0, (availableHeight - contentHeight) / 2);

        gc.setFont(Typography.title());
        gc.setFill(COLOR_TITLE_INK);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText("Gantt Chart - Task Schedule", PADDING, blockTop);

        gc.setFont(Typography.subtitle());
        gc.setFill(COLOR_SUBTITLE_INK);
        String makespanText = current == null ? "searching" : "makespan " + frame.bestMakespan();
        gc.fillText(makespanText + "  ·  " + monitor.processorCount() + " processors"
                + "  ·  " + monitor.taskCount() + " tasks", PADDING, blockTop + TITLE_LINE_HEIGHT);

        double top = blockTop + TITLE_BLOCK_HEIGHT;

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

        // Row labels P1..Pn, tinted to that row's border colour so the mapping to the bars
        // below is unmistakable without a separate legend.
        gc.setFont(Typography.rowLabel());
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setTextBaseline(VPos.CENTER);
        for (int p = 0; p < processorCount; p++) {
            double y = top + p * rowHeight + rowHeight / 2;
            gc.setFill(colorFor(p + 1).border());
            gc.fillText("P" + (p + 1), GUTTER - 6, y);
        }

        // Axis ticks.
        gc.setFont(Typography.axisTick());
        gc.setFill(COLOR_AXIS_TICK);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.TOP);
        double axisY = top + rowHeight * processorCount + 6;
        for (double t = 0; t <= makespan; t += tickStep) {
            double x = barX((int) Math.round(t), xScale, GUTTER);
            gc.fillText(Integer.toString((int) Math.round(t)), x, axisY);
        }

        if (current == null) {
            gc.setFont(Font.font(Typography.UI_FAMILY, FontWeight.NORMAL, 16));
            gc.setFill(COLOR_TITLE_INK);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText("Searching...", GUTTER + plotWidth / 2, top + rowHeight * processorCount / 2);
            return;
        }

        int[] startTimes = current.startTimes();
        int[] processors = current.processors();
        for (int i = 0; i < startTimes.length; i++) {
            drawBar(gc, i, startTimes[i], processors[i], xScale, top, rowHeight, highlighted.contains(i));
        }
    }

    private void drawBar(GraphicsContext gc, int taskIndex, int startTime, int processorNumber,
            double xScale, double top, double rowHeight, boolean highlighted) {
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

        ProcessorColor colors = colorFor(processorNumber);

        gc.setFill(colors.fill());
        gc.fillRoundRect(x, y, w, h, radius * 2, radius * 2);
        gc.setStroke(colors.border());
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, w, h, radius * 2, radius * 2);

        if (highlighted) {
            // Drawn a couple of pixels further outside than the bar's own 2px coloured border,
            // so the two never visually merge - the ring must always read as "the search is
            // reconsidering this task", never as another processor's colour.
            gc.setStroke(COLOR_HIGHLIGHT_RING);
            gc.setLineWidth(2);
            gc.strokeRoundRect(x - 2, y - 2, w + 4, h + 4, radius * 2 + 4, radius * 2 + 4);
        }

        String name = monitor.taskName(taskIndex);
        Text measure = new Text(name);
        measure.setFont(Typography.barLabel());
        double textWidth = measure.getLayoutBounds().getWidth();
        if (textWidth + 8 <= w) {
            gc.setFont(Typography.barLabel());
            gc.setFill(COLOR_BAR_LABEL_INK);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(name, x + w / 2, y + h / 2);
        }
    }

    /**
     * Fill/border pair for a 1-based processor number. Two processors sharing a colour would
     * be actively misleading - worse than neither having one - so beyond the 8th processor,
     * every row falls back to the same neutral pairing (slot 1) rather than cycling the
     * palette; row position (P9, P10, ...) still carries which processor a bar belongs to.
     */
    static ProcessorColor colorFor(int processorNumber) {
        int index = processorNumber - 1;
        return index >= 0 && index < PROCESSOR_COLORS.length ? PROCESSOR_COLORS[index] : PROCESSOR_COLORS[0];
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

    /** A bar fill / border colour pair for one processor row. */
    record ProcessorColor(Color fill, Color border) {
    }
}
