package lavs.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

/** Swaps the root of the single application scene between screens. */
public final class Navigator {

    private static Stage stage;

    private Navigator() {
    }

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    /** @param view file name under /lavs/ui/view without the extension, e.g. "login" */
    public static void show(String view) {
        show(view, controller -> { });
    }

    /**
     * Shows a screen and hands its controller to {@code init} before it is
     * displayed, so the previous screen can pass data across.
     */
    public static <T> void show(String view, Consumer<T> init) {
        String path = "/lavs/ui/view/" + view + ".fxml";
        URL url = Objects.requireNonNull(Navigator.class.getResource(path), "Missing FXML: " + path);
        try {
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            init.accept(loader.getController());
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
            } else {
                // Shortcuts belong to the screen that registered them, not to the shared scene.
                stage.getScene().getAccelerators().clear();
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + path, e);
        }
    }
}
