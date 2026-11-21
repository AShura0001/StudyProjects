package nz.ac.auckland.se206.controllers;

import java.io.IOException;

// nz.ac.auckland.se206.controllers.OpeningController

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.GameTimer;
import nz.ac.auckland.se206.speech.TextToSpeech;

public class OpeningController {

  @FXML private Rectangle aiDefendantRect;
  @FXML private Rectangle aiWitnessRect;
  @FXML private Rectangle humanWitnessRect;
  @FXML private Label timerCounter;
  @FXML private Button submitBtn;

  private static boolean isFirstTimeInit = true;

  @FXML
  public void initialize() {
    GameTimer.getInstance().subscribe("opening", timeString -> {
        timerCounter.setText(timeString);
    });
    
    GameTimer.getInstance().start();
    
    // Play opening TTS only on first visit
    if (isFirstTimeInit) {
      TextToSpeech.speak(
          "This is an AI Courtroom where you have to pass a verdict on the judgement passed by the AI Defendant, Click on each participant below to hear their story and make your decision within 2 minutes provided.");
      isFirstTimeInit = false;
    }
  }
  
  public void cleanup() {
      GameTimer.getInstance().unsubscribe("opening");
      TextToSpeech.stop();
  }

  @FXML
  private void handleRectangleClick(MouseEvent event) throws IOException {
    TextToSpeech.stop();
    try {
    Thread.sleep(50);
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
  }
    Rectangle clickedRectangle = (Rectangle) event.getSource();
    String rectangleId = clickedRectangle.getId();
    App.openChat(event, rectangleId);
}
  
  @FXML
private void passVerdict() throws IOException {
    TextToSpeech.stop();
    // reset timer and then change root
    GameTimer.getInstance().unsubscribe("opening");
    App.setRoot("verdict");
}
}
