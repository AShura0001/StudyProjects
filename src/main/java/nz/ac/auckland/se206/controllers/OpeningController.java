package nz.ac.auckland.se206.controllers;

// nz.ac.auckland.se206.controllers.OpeningController

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.shape.Rectangle;

public class OpeningController {

  @FXML private Rectangle aiDefendantRect;
  @FXML private Rectangle aiWitnessRect;
  @FXML private Rectangle humanWitnessRect;
  @FXML private Label timerCounter;
  @FXML private Button submitBtn;

  private ScheduledExecutorService scheduler;
  private int timeRemaining = 2 * 60;

  @FXML
  public void initialize() {
    startCountdown();
  }

  private void startCountdown() {
    scheduler = Executors.newSingleThreadScheduledExecutor();

    scheduler.scheduleAtFixedRate(
        () -> {
          if (timeRemaining <= 0) {
            scheduler.shutdown();
            Platform.runLater(() -> timerCounter.setText("Time's up!"));
            return;
          }

          timeRemaining--;

          Platform.runLater(
              () -> {
                int minutes = timeRemaining / 60;
                int seconds = timeRemaining % 60;
                timerCounter.setText(String.format("%02d:%02d", minutes, seconds));
              });
        },
        1,
        1,
        TimeUnit.SECONDS);
  }

  @FXML
  private void handleRectangleClick() {}

  @FXML
  private void passVerdict() {}
}
