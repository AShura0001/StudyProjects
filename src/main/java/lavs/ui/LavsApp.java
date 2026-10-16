package lavs.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * JavaFX entry point for the LAVS presentation layer.
 *
 * Screens are loaded from FXML under {@code /lavs/ui/view}. Only the login
 * screen exists so far; the others follow.
 */
public class LavsApp extends Application {

    private static final String LOGIN_VIEW = "/lavs/ui/view/login.fxml";

    @Override
    public void start(Stage stage) throws IOException {
        loadBundledFonts();
        Parent root = FXMLLoader.load(
                Objects.requireNonNull(getClass().getResource(LOGIN_VIEW),
                        "Missing FXML: " + LOGIN_VIEW));

        stage.setTitle("LAVS \u2014 Countrywide Banking Corporation");
        stage.setScene(new Scene(root));
        stage.setMinWidth(760);
        stage.setMinHeight(520);
        stage.show();
    }

    private static void loadBundledFonts() {
        for (String file : new String[] {"Jacquard12-Regular.ttf", "RedHatMono.ttf"}) {
            String path = "/lavs/ui/fonts/" + file;
            try (var in = LavsApp.class.getResourceAsStream(path)) {
                if (in == null || Font.loadFont(in, 12) == null) {
                    System.err.println("Could not load bundled font: " + path);
                }
            } catch (IOException e) {
                System.err.println("Could not load bundled font: " + path);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
