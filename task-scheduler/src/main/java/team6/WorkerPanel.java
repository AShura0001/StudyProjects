package team6;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Right-hand panel showing one box per worker thread: the task it is currently placing,
 * or "idle". Refreshed on demand by the caller (an {@link javafx.animation.AnimationTimer}
 * in {@link Visualiser}).
 */
final class WorkerPanel extends VBox {

    private static final double WIDTH = 220;
    private static final double PADDING = 8;
    private static final double BOX_SPACING = 6;
    private static final double CORNER_RADIUS = 4;
    private static final double DOT_RADIUS = 4;

    private static final Color COLOR_BORDER = Color.web("#E4E4E7");
    private static final Color COLOR_HEADER = Color.web("#52525B");
    private static final Color COLOR_ACTIVE = Color.web("#356094");
    private static final Color COLOR_IDLE = Color.web("#A1A1AA");
    private static final Color COLOR_BODY = Color.web("#18181B");

    private final SearchMonitor monitor;
    private final Circle[] dots;
    private final Text[] bodies;

    WorkerPanel(SearchMonitor monitor) {
        this.monitor = monitor;
        setPrefWidth(WIDTH);
        setSpacing(BOX_SPACING);

        int workerCount = monitor.workerCount();
        dots = new Circle[workerCount];
        bodies = new Text[workerCount];

        for (int w = 0; w < workerCount; w++) {
            getChildren().add(buildBox(w));
        }

        refresh();
    }

    private VBox buildBox(int worker) {
        Text header = new Text("W" + (worker + 1));
        header.setFill(COLOR_HEADER);
        header.setFont(Font.font(11));

        Circle dot = new Circle(DOT_RADIUS);
        dots[worker] = dot;

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerRow = new HBox(header, spacer, dot);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Text body = new Text();
        body.setFont(Font.font("Monospaced", 13));
        bodies[worker] = body;

        VBox box = new VBox(4, headerRow, body);
        box.setPadding(new Insets(PADDING));
        box.setBorder(new Border(new BorderStroke(COLOR_BORDER, BorderStrokeStyle.SOLID,
                new CornerRadii(CORNER_RADIUS), BorderWidths.DEFAULT)));
        return box;
    }

    /** Re-reads the monitor and updates every worker box. Must be called on the JavaFX application thread. */
    void refresh() {
        for (int w = 0; w < dots.length; w++) {
            boolean active = monitor.currentTask(w) >= 0;
            dots[w].setFill(active ? COLOR_ACTIVE : COLOR_IDLE);
            bodies[w].setFill(active ? COLOR_BODY : COLOR_IDLE);
            bodies[w].setText(describeWorker(monitor, w));
        }
    }

    /**
     * Display text for a worker: its current task and processor, or "idle" if it has none.
     * Renders "-" instead of throwing if currentTask() ever returns an index outside the
     * task array, rather than trusting it inside a render loop.
     */
    static String describeWorker(SearchMonitor monitor, int worker) {
        int taskIndex = monitor.currentTask(worker);
        if (taskIndex < 0) {
            return "idle";
        }
        if (taskIndex >= monitor.taskCount()) {
            return "-";
        }
        return monitor.taskName(taskIndex) + " -> P" + monitor.currentProcessor(worker);
    }
}
