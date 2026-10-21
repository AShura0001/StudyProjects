package lavs.ui;

import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import lavs.business.service.AuthService;
import lavs.infrastructure.mainframe.AuthAdapter;
import lavs.ui.controller.*;
import lavs.ui.sync.SyncState;
import mainframe.client.Connection;
import mainframe.client.HttpConnection;

import java.io.IOException;

/**
 * JavaFX entry point for the LAVS presentation layer.
 * <p>
 * Screens are loaded from FXML under {@code /lavs/ui/view} through {@link Navigator}.
 */
public class LavsApp extends Application {

    private static final double CONTENT_WIDTH = 900;
    private static final double CONTENT_HEIGHT = 600;

    private static AppContext context;

    @Override
    public void start(Stage stage) {
        loadBundledFonts();

        String url = System.getProperty("lavs.mainframe.url",
                System.getenv().getOrDefault("LAVS_MAINFRAME_URL", "https://animating-eraser-arrogance.ngrok-free.dev/"));
        Connection connection = new HttpConnection(url);
        context = new AppContext(connection, new AuthService(new AuthAdapter(connection)));
        // -Dlavs.sync=sent|sending|waiting|attention keeps the sync indicator on one state, even after login.
        String preview = System.getProperty("lavs.sync");
        if (preview != null) {
            SyncState previewState = SyncState.valueOf(preview.toUpperCase());
            context.previewSync(previewState, previewState == SyncState.WAITING ? 2 : 1);
        }

        Navigator.init(stage, type -> {
            if (type == LoginController.class) {
                return new LoginController(context.auth(), context::onLoggedIn);
            }
            return Navigator.defaultController(type);   // other screens still use stubs for now
        });
        // Optional first argument jumps straight to a screen, e.g. "loggedout".
        var args = getParameters().getUnnamed();
        showScreen(args.isEmpty() ? "login" : args.get(0));

        stage.setTitle("LAVS — Countrywide Banking Corporation");
        stage.show();

        // The screens are laid out for a 900x600 content area, so the minimum
        // window size adds the title bar and borders on top of that.
        double extraWidth = stage.getWidth() - stage.getScene().getWidth();
        double extraHeight = stage.getHeight() - stage.getScene().getHeight();
        stage.setMinWidth(CONTENT_WIDTH + extraWidth);
        stage.setMinHeight(CONTENT_HEIGHT + extraHeight);
    }

    /**
     * Opens a screen directly. Every screen except login and logged-out reads from the
     * business services, which start at login, so those open the login screen instead.
     */
    public static void showScreen(String name) {
        boolean needsLogin = !name.equals("login") && !name.equals("loggedout");
        if (needsLogin && context.services().isEmpty()) {
            Navigator.show("login");
            return;
        }
        if (name.equals("fulldirectory")) {
            Navigator.<FullDirectoryController>show(name, c -> c.open(0));
        } else {
            Navigator.show(name);
        }
    }

    private static void loadBundledFonts() {
        for (String file : new String[]{"Jacquard12-Regular.ttf", "RedHatMono.ttf", "NATS-Regular.ttf", "Nunito-Bold.ttf", "Nunito-Regular.ttf"}) {
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

    @Override
    public void stop() throws Exception {
        if (context != null) {
            context.close();
        }
    }

    /** For screens that need to log out. */
    public static AppContext context() {
        return context;
    }
}
