package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import nz.ac.auckland.se206.GameTimer;

public class VerdictController {

  @FXML private Label timerCounter;

  @FXML
  public void initialize() {
    // Subscribe to timer updates
    GameTimer.getInstance()
        .subscribe(
            "verdict",
            timeString -> {
              timerCounter.setText(timeString);
            });
  }

  // When navigating away from this scene
  public void cleanup() {
    GameTimer.getInstance().unsubscribe("verdict");
  }
}
