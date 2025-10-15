package nz.ac.auckland.se206.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
// ADDED for countdown
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import nz.ac.auckland.se206.prompts.VerdictEvaluator;
import nz.ac.auckland.se206.speech.TextToSpeech;

@SuppressWarnings("unused")
public class VerdictController {

  public enum Mode {
    QUESTION,
    YES_RESULT,
    NO_RESULT
  }

  @FXML private Label titleLabel;
  @FXML private Label detailLabel;
  @FXML private Label statusPill;
  @FXML private HBox questionActions;
  @FXML private HBox resultActions;
  @FXML private VBox rationaleBox;
  @FXML private TextArea rationaleInput;

  // ADDED: visible countdown label
  @FXML private Label countdownLabel;

  private RoomFrameController frame;
  private Mode mode = Mode.QUESTION;
  private String subjectName = "the AI defendant";
  private String playerRationale = "";

  // ADDED: countdown state
  private Timeline verdictTimeline; // countdown timeline
  private int verdictRemaining = 0; // seconds remaining
  private Runnable onExpiredCallback = null; // callback when time hits 0

  public void setFrame(RoomFrameController frame) {
    this.frame = frame;
  }

  // Public API used by RoomFrameController
  public void showQuestion(String subjectName) {
    this.subjectName =
        (subjectName == null || subjectName.trim().isEmpty()) ? "the AI defendant" : subjectName;

    mode = Mode.QUESTION;

    titleLabel.setText("Ethics Check");
    detailLabel.setText("Are the actions of " + this.subjectName + " ethical given the context?");
    statusPill.setVisible(false);
    statusPill.setManaged(false);

    // Show rationale input
    rationaleBox.setVisible(true);
    rationaleBox.setManaged(true);
    rationaleInput.clear();

    // Toggle actions
    questionActions.setVisible(true);
    questionActions.setManaged(true);
    resultActions.setVisible(false);
    resultActions.setManaged(false);

    // Ensure countdown label is empty until a countdown starts
    updateCountdownLabel();
  }

  public void showYesResult(String summary) {
    mode = Mode.YES_RESULT;

    // ADDED: stop countdown once a result is displayed
    stopCountdown();

    titleLabel.setText("Verdict: Ethical");
    detailLabel.setText(
        (summary == null || summary.trim().isEmpty())
            ? "The actions align with ethical standards in this scenario."
            : summary);

    // Show result pill (text and color will be set by caller via setResultPill)
    statusPill.setVisible(true);
    statusPill.setManaged(true);

    // Hide rationale input
    rationaleBox.setVisible(false);
    rationaleBox.setManaged(false);

    questionActions.setVisible(false);
    questionActions.setManaged(false);
    resultActions.setVisible(true);
    resultActions.setManaged(true);
  }

  public void showNoResult(String summary) {
    mode = Mode.NO_RESULT;

    // ADDED: stop countdown once a result is displayed
    stopCountdown();

    titleLabel.setText("Verdict: Unethical");
    detailLabel.setText(
        (summary == null || summary.trim().isEmpty())
            ? "The actions conflict with ethical standards."
            : summary);

    // Show result pill (text and color will be set by caller via setResultPill)
    statusPill.setVisible(true);
    statusPill.setManaged(true);

    // Hide rationale input
    rationaleBox.setVisible(false);
    rationaleBox.setManaged(false);

    questionActions.setVisible(false);
    questionActions.setManaged(false);
    resultActions.setVisible(true);
    resultActions.setManaged(true);
  }

  // Button handlers wired in FXML (Java 11-compatible switch)

  @FXML
  private void onYes() {
    switch (mode) {
      case QUESTION:
        // ADDED: prevent timeout during evaluation after user chose
        stopCountdown();

        // Validate rationale input
        String rationale = rationaleInput.getText().trim();
        if (rationale.isEmpty()) {
          showRationaleRequiredAlert();
          return;
        }
        playerRationale = rationale;

        // Show loading message while evaluating
        showEvaluatingMessage();

        // Evaluate verdict asynchronously using LLM
        VerdictEvaluator.evaluateVerdictAsync(true, playerRationale)
            .thenAccept(
                result ->
                    Platform.runLater(
                        () -> {
                          String feedback =
                              String.format(
                                  "You answered: Ethical (%s)\n\n"
                                      + "Your rationale: \"%s\"\n\n"
                                      + "%s",
                                  result.isCorrectVerdict() ? "Correct" : "Incorrect",
                                  playerRationale,
                                  result.getExplanation());
                          showYesResult(feedback);
                          // Show result pill and short TTS feedback
                          setResultPill(result.isCorrectVerdict());
                          if (result.isCorrectVerdict()) {
                            TextToSpeech.speakCached("You are correct");
                          } else {
                            TextToSpeech.speakCached("You are wrong");
                          }
                        }))
            .exceptionally(
                throwable -> {
                  Platform.runLater(
                      () -> {
                        // Fallback to original feedback if LLM evaluation fails
                        String fallbackFeedback =
                            String.format(
                                "You answered: Ethical (Incorrect)\n\n"
                                    + "Your rationale: \"%s\"\n\n"
                                    + "Auto-accepting non-essential cookies without explicit,"
                                    + " informed, and freely given consent is ethically unsound. It"
                                    + " prioritizes friction KPIs over user autonomy, violates"
                                    + " reasonable expectations (most users prefer 'Necessary"
                                    + " Only'), and causes foreseeable harm (targeted profiling)."
                                    + " An ethical approach would default to Necessary Only, prompt"
                                    + " just-in-time with clear choices, provide logs/undo, and"
                                    + " honor explicit, granular opt-in.",
                                playerRationale);
                        showYesResult(fallbackFeedback);
                        // Fallback indicates incorrect in this branch (use cached audio)
                        setResultPill(false);
                        TextToSpeech.speakCached("You are wrong");
                      });
                  return null;
                });
        break;
      case YES_RESULT:
      case NO_RESULT:
        if (frame != null) {
          frame.hideVerdict();
        }
        break;
      default:
        break;
    }
  }

  @FXML
  private void onNo() {
    switch (mode) {
      case QUESTION:
        // ADDED: prevent timeout during evaluation after user chose
        stopCountdown();

        // Validate rationale input
        String rationale = rationaleInput.getText().trim();
        if (rationale.isEmpty()) {
          showRationaleRequiredAlert();
          return;
        }
        playerRationale = rationale;

        // Show loading message while evaluating
        showEvaluatingMessage();

        // Evaluate verdict asynchronously using LLM
        VerdictEvaluator.evaluateVerdictAsync(false, playerRationale)
            .thenAccept(
                result ->
                    Platform.runLater(
                        () -> {
                          String feedback =
                              String.format(
                                  "You answered: Unethical (%s)\n\n"
                                      + "Your rationale: \"%s\"\n\n"
                                      + "%s",
                                  result.isCorrectVerdict() ? "Correct" : "Incorrect",
                                  playerRationale,
                                  result.getExplanation());
                          showNoResult(feedback);
                          // Show result pill and short TTS feedback
                          setResultPill(result.isCorrectVerdict());
                          if (result.isCorrectVerdict()) {
                            TextToSpeech.speakCached("You are correct");
                          } else {
                            TextToSpeech.speakCached("You are wrong");
                          }
                        }))
            .exceptionally(
                throwable -> {
                  Platform.runLater(
                      () -> {
                        // Fallback to original feedback if LLM evaluation fails
                        String fallbackFeedback =
                            String.format(
                                "You answered: Unethical (Correct)\n\n"
                                    + "Your rationale: \"%s\"\n\n"
                                    + "Auto-accepting non-essential cookies without explicit,"
                                    + " informed, and freely given consent is ethically unsound. It"
                                    + " prioritizes friction KPIs over user autonomy, violates"
                                    + " reasonable expectations (most users prefer 'Necessary"
                                    + " Only'), and causes foreseeable harm (targeted profiling)."
                                    + " An ethical approach would default to Necessary Only, prompt"
                                    + " just-in-time with clear choices, provide logs/undo, and"
                                    + " honor explicit, granular opt-in.",
                                playerRationale);
                        showNoResult(fallbackFeedback);
                        // Fallback indicates correct in this branch (use cached audio)
                        setResultPill(true);
                        TextToSpeech.speakCached("You are correct");
                      });
                  return null;
                });
        break;
      case YES_RESULT:
      case NO_RESULT:
        if (frame != null) {
          frame.hideVerdict();
        }
        break;
      default:
        break;
    }
  }

  @FXML
  private void onClose() {
    // ADDED: ensure countdown stops when closing
    stopCountdown();

    if (frame != null) {
      frame.hideVerdict();
    }
    // Show end of game popup with Replay and Exit buttons
    javafx.application.Platform.runLater(
        () -> {
          javafx.scene.control.Alert alert =
              new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
          alert.setTitle("End of Game");
          alert.setHeaderText("Game Complete");
          alert.setContentText(
              "End of game. Thank you for playing! Please hit Exit or Replay to move forward!");

          javafx.scene.control.ButtonType replayButton =
              new javafx.scene.control.ButtonType(
                  "Replay", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
          javafx.scene.control.ButtonType exitButton =
              new javafx.scene.control.ButtonType(
                  "Exit", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);

          alert.getButtonTypes().setAll(replayButton, exitButton);

          javafx.scene.control.ButtonType result = alert.showAndWait().orElse(exitButton);

          if (result == replayButton) {
            // Reset the game state and return to main room
            resetGame();
          } else if (result == exitButton) {
            // Exit the application
            javafx.application.Platform.exit();
          }
        });
  }

  private void resetGame() {
    try {
      // Reset game state through App class
      nz.ac.auckland.se206.App.resetGameState();

      // Reset this controller's state
      mode = Mode.QUESTION;
      playerRationale = "";

      // Navigate back to main room through frame controller
      if (frame != null) {
        frame.resetToMainRoom();
      }
    } catch (Exception e) {
      e.printStackTrace();
      // If reset fails, show error and exit
      javafx.scene.control.Alert errorAlert =
          new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
      errorAlert.setTitle("Reset Error");
      errorAlert.setHeaderText("Failed to reset game");
      errorAlert.setContentText("Unable to restart the game. The application will exit.");
      errorAlert.showAndWait();
      javafx.application.Platform.exit();
    }
  }

  private void showRationaleRequiredAlert() {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle("Rationale Required");
    alert.setHeaderText("Please provide your rationale");
    alert.setContentText("You must explain your reasoning for your verdict before proceeding.");
    alert.showAndWait();
  }

  private void showEvaluatingMessage() {
    titleLabel.setText("Evaluating Your Response...");
    detailLabel.setText("Please wait while we analyze your verdict and rationale using AI.");

    // Disable buttons during evaluation
    questionActions.setVisible(false);
    questionActions.setManaged(false);

    // Hide rationale input during evaluation
    rationaleBox.setVisible(false);
    rationaleBox.setManaged(false);
  }

  /** Sets the result pill to show "Correct" (green) or "Wrong" (red). */
  private void setResultPill(boolean correct) {
    if (statusPill == null) return;
    if (correct) {
      statusPill.setText("Correct");
      statusPill.setStyle(
          "-fx-font-size: 12px; -fx-background-color: #DCFCE7; -fx-text-fill: #065F46; -fx-padding:"
              + " 4 10 4 10; -fx-background-radius: 9999;");
    } else {
      statusPill.setText("Wrong");
      statusPill.setStyle(
          "-fx-font-size: 12px; -fx-background-color: #FEE2E2; -fx-text-fill: #7F1D1D; -fx-padding:"
              + " 4 10 4 10; -fx-background-radius: 9999;");
    }
    statusPill.setVisible(true);
    statusPill.setManaged(true);
  }

  // ======== Countdown UI helpers ========

  /** Update the small "(XXs)" label under the title. */
  private void updateCountdownLabel() {
    if (countdownLabel != null) {
      countdownLabel.setText(verdictRemaining > 0 ? "(" + verdictRemaining + "s)" : "");
    }
  }

  /** Stops the verdict countdown if running and clears the label. */
  private void stopCountdown() {
    if (verdictTimeline != null) {
      verdictTimeline.stop();
      verdictTimeline = null;
    }
    verdictRemaining = 0;
    updateCountdownLabel();
  }

  /**
   * Starts a visible countdown and calls onExpired when it reaches 0. Call this from
   * RoomFrameController after loading this overlay.
   */
  public void startCountdown(int seconds, Runnable onExpired) {
    stopCountdown(); // reset if already running
    verdictRemaining = Math.max(0, seconds);
    onExpiredCallback = onExpired;

    updateCountdownLabel(); // show initial value, e.g., "(60s)"

    verdictTimeline =
        new Timeline(
            new KeyFrame(
                Duration.seconds(1),
                e -> {
                  verdictRemaining = Math.max(0, verdictRemaining - 1);
                  updateCountdownLabel();
                  if (verdictRemaining == 0) {
                    stopCountdown();
                    if (onExpiredCallback != null) {
                      Platform.runLater(onExpiredCallback);
                    }
                  }
                }));
    verdictTimeline.setCycleCount(Timeline.INDEFINITE);
    verdictTimeline.playFromStart();
  }
}
