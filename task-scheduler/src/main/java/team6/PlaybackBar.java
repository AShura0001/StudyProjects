package team6;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

/**
 * Playback controls for a completed search: a tick strip showing where the improvements
 * landed, above a scrub slider and transport controls. Hidden and unmanaged until
 * {@link #reveal} is called, so it takes no space during a live run.
 *
 * <p>This class only owns the controls and their visual state - it fires no behaviour of its
 * own. {@link Visualiser} attaches the actual play/pause/step/scrub logic to the buttons,
 * slider and choice box exposed here as package-private fields.
 */
final class PlaybackBar extends VBox {

    private static final double TICK_HEIGHT = 8;
    private static final double THUMB_INSET = 7; // approximates the default slider thumb radius
    private static final double PADDING = 8;

    private static final Color COLOR_BACKGROUND = Color.web("#FAFAFA");
    private static final Color COLOR_BORDER = Color.web("#E4E4E7");
    private static final Color COLOR_TICK = Color.web("#C2410C");

    private static final String PLAY_GLYPH = "▶";
    private static final String PAUSE_GLYPH = "❚❚";

    final Button prevImprovementButton = new Button("|◀");
    final Button playPauseButton = new Button(PLAY_GLYPH);
    final Button nextImprovementButton = new Button("▶|");
    final Slider slider = new Slider();
    final ChoiceBox<Double> speedChoice = new ChoiceBox<>();

    private final Label timeLabel = new Label();
    private final Canvas tickCanvas = new Canvas();

    private Timeline timeline;
    private boolean suppressSliderEvent = false;

    PlaybackBar() {
        setPadding(new Insets(PADDING));
        setBackground(new Background(new BackgroundFill(COLOR_BACKGROUND, CornerRadii.EMPTY, Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(COLOR_BORDER, COLOR_BORDER, COLOR_BORDER, COLOR_BORDER,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE,
                CornerRadii.EMPTY, new BorderWidths(1, 0, 0, 0), Insets.EMPTY)));

        slider.setMaxWidth(Double.MAX_VALUE);
        tickCanvas.setHeight(TICK_HEIGHT);
        tickCanvas.widthProperty().bind(slider.widthProperty());
        tickCanvas.widthProperty().addListener((obs, oldWidth, newWidth) -> drawTicks());

        VBox sliderGroup = new VBox(2, tickCanvas, slider);
        HBox.setHgrow(sliderGroup, Priority.ALWAYS);

        timeLabel.setFont(Font.font("Monospaced", 12));
        timeLabel.setText(StatsPanel.formatElapsed(0) + " / " + StatsPanel.formatElapsed(0));

        speedChoice.getItems().addAll(0.5, 1.0, 2.0, 4.0);
        speedChoice.setValue(1.0);
        speedChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(Double speed) {
                return speed == null ? "" : formatSpeed(speed);
            }

            @Override
            public Double fromString(String text) {
                return null; // the choice box is not editable, so parsing is never needed
            }
        });

        HBox controlsRow = new HBox(8, prevImprovementButton, playPauseButton, nextImprovementButton,
                sliderGroup, timeLabel, speedChoice);
        controlsRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().add(controlsRow);

        setVisible(false);
        setManaged(false);
    }

    /** Shows the bar, sized for the given timeline; disables the slider and play button if it has fewer than 2 frames. */
    void reveal(Timeline timeline) {
        this.timeline = timeline;
        int size = timeline.size();
        slider.setMin(0);
        slider.setMax(Math.max(0, size - 1));

        boolean scrubbable = size >= 2;
        slider.setDisable(!scrubbable);
        playPauseButton.setDisable(!scrubbable);

        drawTicks();
        setVisible(true);
        setManaged(true);
    }

    /**
     * Moves the slider to the given frame index without firing the slider's own listener (see
     * {@link #isSuppressingSliderEvent}), and updates the time label from that frame.
     */
    void showIndex(int index) {
        suppressSliderEvent = true;
        slider.setValue(index);
        suppressSliderEvent = false;

        if (timeline != null && index >= 0 && index < timeline.size()) {
            Timeline.Frame frame = timeline.frame(index);
            timeLabel.setText(StatsPanel.formatElapsed(frame.elapsedMillis())
                    + " / " + StatsPanel.formatElapsed(timeline.durationMillis()));
        }
    }

    /**
     * Whether the slider's value is currently being set programmatically by {@link #showIndex}.
     * The slider's value-change listener (attached externally) must check this and return
     * immediately when true, or a programmatic move would re-trigger the same rendering and
     * playback re-anchoring a user drag triggers, forming a feedback loop.
     */
    boolean isSuppressingSliderEvent() {
        return suppressSliderEvent;
    }

    /** Swaps the play/pause button's glyph to match whether playback is currently running. */
    void setPlaying(boolean playing) {
        playPauseButton.setText(playing ? PAUSE_GLYPH : PLAY_GLYPH);
    }

    private void drawTicks() {
        GraphicsContext gc = tickCanvas.getGraphicsContext2D();
        double width = tickCanvas.getWidth();
        double height = tickCanvas.getHeight();
        gc.clearRect(0, 0, width, height);

        if (timeline == null || width <= 0) {
            return;
        }

        int frameCount = timeline.size();
        gc.setFill(COLOR_TICK);
        for (int index : timeline.improvementIndices()) {
            double x = tickX(index, frameCount, width, THUMB_INSET);
            gc.fillRect(x - 1, 0, 2, height);
        }
    }

    private static String formatSpeed(double speed) {
        if (speed == Math.rint(speed)) {
            return (long) speed + "x";
        }
        return speed + "x";
    }

    /**
     * X coordinate, within a track of the given width, that a slider's thumb sits at for the
     * given frame index out of frameCount total - the same travel the slider control itself
     * uses (thumbInset at index 0, trackWidth - thumbInset at the last index), so a tick drawn
     * here lines up with where the thumb passes over it. Never divides by zero: a single-frame
     * timeline has nowhere to travel, so it returns thumbInset for every index.
     */
    static double tickX(int index, int frameCount, double trackWidth, double thumbInset) {
        if (frameCount <= 1) {
            return thumbInset;
        }
        double fraction = index / (double) (frameCount - 1);
        return thumbInset + fraction * (trackWidth - 2 * thumbInset);
    }
}
