package lavs.ui.component;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.Pane;

/**
 * A small "Press F1 for shortcuts" note in the bottom right corner of a screen.
 * Clicking it does the same as pressing F1. It floats over the screen without
 * taking part in the layout, so no screen changes size or shifts to make room.
 * Added from code so the FXML files stay plain.
 */
public final class ShortcutHint extends Label {

    private static final double MARGIN_RIGHT = 12;
    private static final double MARGIN_BOTTOM = 3;

    private ShortcutHint() {
        super("Press F1 for shortcuts");
        getStyleClass().add("shortcut-hint");
        setManaged(false);
        setTooltip(new Tooltip("Show the keyboard shortcuts for this screen (F1)"));
        setOnMouseClicked(e -> {
            if (getScene() == null) {
                return;
            }
            Runnable showShortcuts = getScene().getAccelerators().get(new KeyCodeCombination(KeyCode.F1));
            if (showShortcuts != null) {
                showShortcuts.run();
            }
        });
    }

    /** Adds the hint to a screen whose root is a Pane; any other root is left alone. */
    public static void addTo(Parent root) {
        if (!(root instanceof Pane pane)) {
            return;
        }
        ShortcutHint hint = new ShortcutHint();
        pane.getChildren().add(hint);
        Runnable place = () -> {
            // Unmanaged nodes are not sized by their parent, and need their style
            // applied first or they measure as empty.
            hint.applyCss();
            hint.resize(hint.prefWidth(-1), hint.prefHeight(-1));
            hint.relocate(pane.getWidth() - hint.getWidth() - MARGIN_RIGHT,
                    pane.getHeight() - hint.getHeight() - MARGIN_BOTTOM);
        };
        pane.widthProperty().addListener((obs, old, now) -> place.run());
        pane.heightProperty().addListener((obs, old, now) -> place.run());
        hint.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                place.run();
            }
        });
        place.run();
    }
}
