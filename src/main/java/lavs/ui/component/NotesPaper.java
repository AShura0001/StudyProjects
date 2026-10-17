package lavs.ui.component;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/**
 * Gives a notes text area a lined-paper background. The lines are drawn
 * behind the text at exactly the text's line height and move with it when the
 * text scrolls. It is applied from code so the FXML stays a plain TextArea and
 * still opens in Scene Builder.
 */
public final class NotesPaper {

    private NotesPaper() {
    }

    public static void attach(TextArea area) {
        area.getStyleClass().add("live");
        Pane parent = (Pane) area.getParent();
        int index = parent.getChildren().indexOf(area);

        Region lines = new Region();
        lines.setMouseTransparent(true);
        lines.getStyleClass().add("notes-lines");
        StackPane.setAlignment(lines, Pos.TOP_LEFT);

        StackPane paper = new StackPane();
        paper.getStyleClass().add("notes-paper");
        VBox.setVgrow(paper, Priority.ALWAYS);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(paper.widthProperty());
        clip.heightProperty().bind(paper.heightProperty());
        paper.setClip(clip);

        parent.getChildren().remove(area);
        paper.getChildren().addAll(lines, area);
        parent.getChildren().add(index, paper);

        double[] pitch = {16};
        lines.prefHeightProperty().bind(paper.heightProperty().add(40));
        lines.maxHeightProperty().bind(lines.prefHeightProperty());

        area.scrollTopProperty().addListener((obs, old, now) ->
                lines.setTranslateY(-(now.doubleValue() % pitch[0])));

        Runnable measure = () -> {
            area.applyCss();
            // The text area rounds each line up to a whole pixel (a measured 15.004 lays
            // out at 16), so the ruled lines have to use the rounded-up pitch to stay level.
            Text oneLine = new Text("Ag");
            Text twoLines = new Text("Ag\nAg");
            oneLine.setFont(area.getFont());
            twoLines.setFont(area.getFont());
            double measured = twoLines.getLayoutBounds().getHeight() - oneLine.getLayoutBounds().getHeight();
            pitch[0] = Math.max(8, Math.ceil(measured));
            double thin = (pitch[0] - 1) / pitch[0] * 100;
            lines.setStyle("-fx-background-color: linear-gradient(from 0px 0px to 0px " + pitch[0]
                    + "px, repeat, transparent 0%, transparent " + thin + "%, -lavs-line " + thin
                    + "%, -lavs-line 100%);");
        };
        paper.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                Platform.runLater(measure);
            }
        });
        area.fontProperty().addListener((obs, old, now) -> measure.run());
    }
}
