package nz.ac.auckland.se206;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javafx.application.Platform;

public class GameTimer {
  private static final GameTimer INSTANCE = new GameTimer();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  // Map of scene controllers listening to timer updates
  private final ConcurrentHashMap<String, Consumer<String>> subscribers = new ConcurrentHashMap<>();

  private int totalSeconds = 120; // 2 minutes 
  private int graceSeconds = 10; // 10 second final verdict time
  private boolean isRunning = false;
  // Prevent multiple auto-navigations to verdict
  private boolean hasNavigated = false;

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
    hasNavigated = false; // flag for the verdict screen
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
          } else if (graceSeconds > 0) {
            // open verdict scene when grace period starts
            if (!hasNavigated) {
              hasNavigated = true;
              Platform.runLater(
                  () -> {
                    try {
                      App.setRoot("verdict");
                    } catch (IOException e) {
                      System.err.println("Failed to navigate to verdict: " + e.getMessage());
                    }
                  });
            }

            // Continue grace period countdown IN verdict scene
            graceSeconds--;
            String graceTime = "FINAL: " + String.format("%02d", graceSeconds);

            Platform.runLater(
                () -> {
                  subscribers.values().forEach(callback -> callback.accept(graceTime));
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
    scheduler.shutdownNow();
  }

  public void forceTimeUp() {
    isRunning = false;
    scheduler.shutdownNow();
    // Update all subscribers to show TIME UP
    Platform.runLater(
        () -> {
          subscribers.values().forEach(callback -> callback.accept("TIME UP!"));
        });
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
