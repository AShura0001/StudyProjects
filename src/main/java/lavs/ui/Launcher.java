package lavs.ui;

/**
 * Plain entry point that does not extend {@code Application}.
 *
 * Launching through this class lets the app start from a normal classpath run
 * (IntelliJ's green arrow, {@code java -cp ...}) without the "JavaFX runtime
 * components are missing" error. {@code mvn javafx:run} uses LavsApp directly.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        LavsApp.main(args);
    }
}
