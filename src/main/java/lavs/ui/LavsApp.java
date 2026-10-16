package lavs.ui;

import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import lavs.ui.controller.CustomerDetailsController;
import lavs.ui.controller.FullDirectoryController;
import lavs.ui.controller.LoanDetailsController;
import lavs.ui.controller.SearchResultsController;
import lavs.ui.stub.StubCustomerService;

import java.io.IOException;
import java.util.List;

/**
 * JavaFX entry point for the LAVS presentation layer.
 *
 * Screens are loaded from FXML under {@code /lavs/ui/view} through {@link Navigator}.
 */
public class LavsApp extends Application {

    private static final double CONTENT_WIDTH = 900;
    private static final double CONTENT_HEIGHT = 600;

    @Override
    public void start(Stage stage) {
        loadBundledFonts();
        Navigator.init(stage);

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
     * Opens a screen directly. Screens that normally receive their data from
     * the previous screen are given sample data so they are not blank.
     */
    public static void showScreen(String name) {
        switch (name) {
            case "searchresults" -> Navigator.<SearchResultsController>show(name, c -> c.show("JANE DOE", List.of(
                    new StubCustomerService.Match("Jane Doe", "CUS-20481"),
                    new StubCustomerService.Match("Mary Jane", "CUS-20482"),
                    new StubCustomerService.Match("Sarah Doe Jr. Watkins", "CUS-20483"))));
            case "fulldirectory" -> Navigator.<FullDirectoryController>show(name, c -> c.open(0));
            case "customerdetails" -> Navigator.<CustomerDetailsController>show(name,
                    c -> c.open("CUS-20490", () -> Navigator.show("customersearch")));
            case "loandetails" -> Navigator.<LoanDetailsController>show(name,
                    c -> c.open("LN-5004", () -> Navigator.show("customersearch")));
            default -> Navigator.show(name);
        }
    }

    private static void loadBundledFonts() {
        for (String file : new String[] {"Jacquard12-Regular.ttf", "RedHatMono.ttf", "NATS-Regular.ttf", "Nunito-Bold.ttf", "Nunito-Regular.ttf"}) {
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
