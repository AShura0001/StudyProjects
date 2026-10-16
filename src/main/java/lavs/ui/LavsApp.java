package lavs.ui;

import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX entry point for the LAVS presentation layer.
 *
 * Screens are loaded from FXML under {@code /lavs/ui/view} through {@link Navigator}.
 */
public class LavsApp extends Application {

        @Override
    public void start(Stage stage) {
        loadBundledFonts();
        Navigator.init(stage);

        // Optional first argument jumps straight to a screen, e.g. "loggedout".
        var args = getParameters().getUnnamed();
        Navigator.show(args.isEmpty() ? "login" : args.get(0));

        stage.setTitle("LAVS — Countrywide Banking Corporation");
        stage.setWidth(900);
        stage.setHeight(600);
        stage.setMinWidth(760);
        stage.setMinHeight(520);
        stage.show();
    }

    private static void loadBundledFonts() {
        for (String file : new String[] {"Jacquard12-Regular.ttf", "RedHatMono.ttf", "NATS-Regular.ttf", "Nunito-Bold.ttf"}) {
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
