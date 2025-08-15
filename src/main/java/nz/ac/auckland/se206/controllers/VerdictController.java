package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import nz.ac.auckland.se206.GameTimer;

public class VerdictController {

  @FXML private Label timerCounter;
  @FXML private Button yesBtn;
  @FXML private Button noBtn;
  @FXML private TextArea reasoning;
  @FXML private Label result;

  private boolean choiceMade = false;

  @FXML
  public void initialize() {
    reasoning.setEditable(false);
    reasoning.setWrapText(true);

    // Subscribe to timer to show grace period countdown
    GameTimer.getInstance()
        .subscribe(
            "verdict",
            timeString -> {
              timerCounter.setText(timeString);

              // If time is completely up and no choice made, show explanation
              if ("TIME UP!".equals(timeString) && !choiceMade) {
                showExplanation();
              }
            });
  }

  @FXML
  private void agree() {
    if (!choiceMade) {
      choiceMade = true;
      result.setText("You passed the wrong Verdict!");
      showExplanation();
      GameTimer.getInstance().forceTimeUp();
    }
  }

  @FXML
  private void donotagree() {
    if (!choiceMade) {
      choiceMade = true;
      result.setText("You passed the correct Verdict!");
      showExplanation();
      GameTimer.getInstance().forceTimeUp();
    }
  }

  private void showExplanation() {
    reasoning.setText(
        "REASONING: \n"
            + "The judge made a wrong decision since it had a bias influenced by personal benefits"
            + " in the development of robots and also didn't properly consider other facts shown in"
            + " the case");

    // Disable buttons after choice is made
    yesBtn.setDisable(true);
    noBtn.setDisable(true);
  }

  public void cleanup() {
    GameTimer.getInstance().unsubscribe("verdict");
  }
}
