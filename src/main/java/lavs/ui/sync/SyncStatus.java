package lavs.ui.sync;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * What the sync indicator shows. Safe to update from any thread; listeners on
 * the properties always run on the JavaFX thread.
 */
public final class SyncStatus {

    private final ObjectProperty<SyncState> state = new SimpleObjectProperty<>(SyncState.SENT);
    private final IntegerProperty count = new SimpleIntegerProperty(0);

    public ObjectProperty<SyncState> stateProperty() {
        return state;
    }

    public IntegerProperty countProperty() {
        return count;
    }

    public SyncState state() {
        return state.get();
    }

    public int count() {
        return count.get();
    }

    /** @param count how many changes the state is about; ignored for SENT and SENDING */
    public void set(SyncState newState, int count) {
        Runnable apply = () -> {
            this.count.set(Math.max(0, count));
            this.state.set(newState);
        };
        if (Platform.isFxApplicationThread()) {
            apply.run();
        } else {
            Platform.runLater(apply);
        }
    }
}
