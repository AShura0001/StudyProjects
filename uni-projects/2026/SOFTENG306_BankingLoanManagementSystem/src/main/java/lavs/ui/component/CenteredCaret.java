package lavs.ui.component;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javafx.scene.shape.Path;

/**
 * Keeps the blinking caret in the middle of an empty, centre-aligned text field.
 * JavaFX centres the placeholder but leaves the caret at the left edge until
 * the first character is typed; this moves it to the middle while the field is
 * empty and puts it back as soon as there is text.
 */
public final class CenteredCaret {

    private CenteredCaret() {
    }

    public static void apply(TextInputControl field) {
        Runnable update = () -> shift(field);
        field.textProperty().addListener((obs, old, now) -> Platform.runLater(update));
        field.focusedProperty().addListener((obs, old, now) -> Platform.runLater(update));
        field.widthProperty().addListener((obs, old, now) -> Platform.runLater(update));
        field.skinProperty().addListener((obs, old, now) -> Platform.runLater(update));
        Platform.runLater(update);
    }

    private static void shift(TextInputControl field) {
        Path caret = caretOf(field);
        if (caret == null) {
            return;
        }
        boolean empty = field.getText() == null || field.getText().isEmpty();
        if (!empty) {
            caret.setTranslateX(0);
            return;
        }
        // Work from where the caret really is, not where it should be: after text
        // is cleared JavaFX can leave it somewhere other than the left edge.
        double unshifted = caret.localToScene(caret.getBoundsInLocal()).getMinX() - caret.getTranslateX();
        double middle = field.localToScene(field.getBoundsInLocal()).getCenterX();
        caret.setTranslateX(middle - unshifted);
    }

    private static Path caretOf(TextInputControl field) {
        for (Node node : field.lookupAll("*")) {
            if (node instanceof Path path && !path.isManaged() && path.getStroke() != null) {
                return path;
            }
        }
        return null;
    }
}
