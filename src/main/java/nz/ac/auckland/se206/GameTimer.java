package nz.ac.auckland.se206;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javafx.application.Platform;

public class GameTimer {
  private static final GameTimer INSTANCE = new GameTimer();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final ConcurrentHashMap<String, Consumer<String>> subscribers = new ConcurrentHashMap<>();

  private int totalSeconds = 120; // 2 minutes
  private boolean isRunning = false;

  private GameTimer() {}

  public static GameTimer getInstance() {
    return INSTANCE;
  }

  /** Subscribe a controller to receive timer updates */
  public void subscribe(String controllerId, Consumer<String> updateCallback) {
    subscribers.put(controllerId, updateCallback);
    // Send current time immediately
    updateCallback.accept(formatTime(totalSeconds));
  }

  /** Unsubscribe a controller from timer updates */
  public void unsubscribe(String controllerId) {
    subscribers.remove(controllerId);
  }

  /** Start the timer */
  public void start() {
    if (isRunning) return;

    isRunning = true;
    scheduler.scheduleAtFixedRate(
        () -> {
          if (totalSeconds > 0) {
            totalSeconds--;
            String timeString = formatTime(totalSeconds);

            // Update all subscribers on JavaFX thread
            Platform.runLater(
                () -> {
                  subscribers.values().forEach(callback -> callback.accept(timeString));
                });
          } else {
            // Timer finished
            stop();
            Platform.runLater(
                () -> {
                  subscribers.values().forEach(callback -> callback.accept("TIME UP!"));
                });
          }
        },
        1,
        1,
        TimeUnit.SECONDS);
  }

  /** Stop the timer */
  public void stop() {
    isRunning = false;
  }

  /** Reset timer to initial value */
  public void reset() {
    stop();
    totalSeconds = 120;
    String timeString = formatTime(totalSeconds);
    Platform.runLater(
        () -> {
          subscribers.values().forEach(callback -> callback.accept(timeString));
        });
  }

  /** Get current time remaining */
  public int getTimeRemaining() {
    return totalSeconds;
  }

  /** Check if timer is running */
  public boolean isRunning() {
    return isRunning;
  }

  /** Format seconds to MM:SS */
  private String formatTime(int seconds) {
    int minutes = seconds / 60;
    int remainingSeconds = seconds % 60;
    return String.format("%02d:%02d", minutes, remainingSeconds);
  }

  /** Shutdown the timer service */
  public void shutdown() {
    scheduler.shutdown();
    subscribers.clear();
  }
}
