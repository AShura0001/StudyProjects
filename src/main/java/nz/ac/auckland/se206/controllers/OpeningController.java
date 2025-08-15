package nz.ac.auckland.se206.controllers;

import java.io.IOException;

// nz.ac.auckland.se206.controllers.OpeningController

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import nz.ac.auckland.se206.GameStateContext;
import nz.ac.auckland.se206.GameTimer;

public class OpeningController {

  @FXML private Rectangle aiDefendantRect;
  @FXML private Rectangle aiWitnessRect;
  @FXML private Rectangle humanWitnessRect;
  @FXML private Label timerCounter;
  @FXML private Button submitBtn;

  private static GameStateContext context = new GameStateContext();

  @FXML
  public void initialize() {
    // Subscribe to timer updates
    GameTimer.getInstance().subscribe("opening", timeString -> {
        timerCounter.setText(timeString);
    });
    
    // Start the timer when opening scene loads
    GameTimer.getInstance().start();
  }
  
  // When navigating away from this scene
  public void cleanup() {
      GameTimer.getInstance().unsubscribe("opening");
  }

  @FXML
  private void handleRectangleClick(MouseEvent event) throws IOException{
    Rectangle clickedRectangle = (Rectangle) event.getSource();
    context.handleRectangleClick(event, clickedRectangle.getId());
  }

  @FXML
  private void passVerdict() {}
}
