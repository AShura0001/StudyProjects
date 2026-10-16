package lavs.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;

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
        String path = "/lavs/ui/view/" + view + ".fxml";
        URL url = Objects.requireNonNull(Navigator.class.getResource(path), "Missing FXML: " + path);
        try {
            Parent root = FXMLLoader.load(url);
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
            } else {
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + path, e);
        }
    }
}
