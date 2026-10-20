package lavs.ui.component;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import lavs.ui.sync.SyncState;
import lavs.ui.sync.SyncStatus;

/**
 * Three dots in the top bar showing how far the sync of saved changes has got:
 * three green when everything is sent, a white dot moving along when sending,
 * two yellow when changes are waiting, one red when something needs checking.
 * Clicking it explains the state. Added from code so the FXML stays plain.
 */
public final class SyncIndicator extends HBox {

    private static final String[] SENT = {"sync-dot-sent", "sync-dot-sent", "sync-dot-sent"};
    private static final String[] WAITING = {"sync-dot-waiting", "sync-dot-waiting", "sync-dot-empty"};
    private static final String[] ATTENTION = {"sync-dot-attention", "sync-dot-empty", "sync-dot-empty"};

    private final SyncStatus status;
    private final Circle[] dots = {new Circle(4), new Circle(4), new Circle(4)};
    private final Tooltip tooltip = new Tooltip();
    private final Timeline blink = new Timeline();
    private int lit;

    public SyncIndicator(SyncStatus status) {
        super(5);
        this.status = status;
        getStyleClass().add("sync-pill");
        setAlignment(Pos.CENTER);
        setMinHeight(20);
        setPrefHeight(20);
        setMaxHeight(20);
        setPadding(new Insets(0, 8, 0, 8));
        getChildren().addAll(dots);
        Tooltip.install(this, tooltip);
        setOnMouseClicked(e -> explain());

        blink.setCycleCount(Timeline.INDEFINITE);
        blink.getKeyFrames().add(new KeyFrame(Duration.millis(380), e -> {
            lit = (lit + 1) % dots.length;
            showSending();
        }));

        status.stateProperty().addListener((obs, old, now) -> render());
        status.countProperty().addListener((obs, old, now) -> render());
        // The animation only runs while the indicator is actually on a screen.
        sceneProperty().addListener((obs, old, scene) -> render());
        render();
    }

    /**
     * Puts an indicator in front of the Logout button of a screen that has one;
     * screens without that top bar, such as Login, are left alone.
     */
    public static void addTo(Parent root, SyncStatus status) {
        if (!(root instanceof BorderPane pane) || !(pane.getTop() instanceof HBox bar)) {
            return;
        }
        for (int i = 0; i < bar.getChildren().size(); i++) {
            Node child = bar.getChildren().get(i);
            if (child.getStyleClass().contains("btn-chip-danger")) {
                SyncIndicator indicator = new SyncIndicator(status);
                HBox.setMargin(indicator, new Insets(0, 8, 0, 0));
                bar.getChildren().add(i, indicator);
                return;
            }
        }
    }

    private void render() {
        SyncState state = status.state();
        blink.stop();
        switch (state) {
            case SENT -> apply(SENT);
            case WAITING -> apply(WAITING);
            case ATTENTION -> apply(ATTENTION);
            case SENDING -> {
                lit = 0;
                showSending();
                if (getScene() != null) {
                    blink.play();
                }
            }
        }
        tooltip.setText(summary(state, status.count()));
    }

    private void apply(String[] styles) {
        for (int i = 0; i < dots.length; i++) {
            dots[i].getStyleClass().setAll("sync-dot", styles[i]);
        }
    }

    private void showSending() {
        for (int i = 0; i < dots.length; i++) {
            dots[i].getStyleClass().setAll("sync-dot", i == lit ? "sync-dot-sending" : "sync-dot-idle");
        }
    }

    private static String summary(SyncState state, int count) {
        String changes = count == 1 ? "1 change" : count + " changes";
        return switch (state) {
            case SENT -> "All changes sent";
            case SENDING -> "Sending changes…";
            case WAITING -> changes + " not yet sent";
            case ATTENTION -> changes + " need" + (count == 1 ? "s" : "") + " checking";
        };
    }

    private void explain() {
        SyncState state = status.state();
        int count = status.count();
        String changes = count == 1 ? "1 change" : count + " changes";
        String text = switch (state) {
            case SENT -> "Every change you have made has been sent to the mainframe.";
            case SENDING -> "Your changes are being sent to the mainframe now.";
            case WAITING -> changes + " could not be sent because the mainframe cannot be reached. "
                    + "They are saved on this computer and will be sent automatically when the connection returns.";
            case ATTENTION -> changes + (count == 1 ? " needs" : " need") + " checking. The mainframe may or may not have "
                    + "received " + (count == 1 ? "it" : "them") + ", or it was refused. Ask a teammate before entering it again.";
        };
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sync status");
        alert.setHeaderText(summary(state, count));
        alert.setContentText(text);
        if (getScene() != null) {
            alert.initOwner(getScene().getWindow());
        }
        alert.show();
    }
}
