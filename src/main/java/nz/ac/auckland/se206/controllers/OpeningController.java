package nz.ac.auckland.se206.controllers;

import java.io.IOException;

// nz.ac.auckland.se206.controllers.OpeningController

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import nz.ac.auckland.se206.GameStateContext;

public class OpeningController {

  @FXML private Rectangle aiDefendantRect;
  @FXML private Rectangle aiWitnessRect;
  @FXML private Rectangle humanWitnessRect;
  @FXML private Label timerCounter;
  @FXML private Button submitBtn;

  private static GameStateContext context = new GameStateContext();
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
  private void handleRectangleClick(MouseEvent event) throws IOException{
    Rectangle clickedRectangle = (Rectangle) event.getSource();
    context.handleRectangleClick(event, clickedRectangle.getId());
  }

  @FXML
  private void passVerdict() {}
}
