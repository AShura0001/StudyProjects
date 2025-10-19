package nz.ac.auckland.se206.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
// ADDED for countdown
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
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
    NO_RESULT,
    TIMEOUT_RESULT
  }

  private enum VerdictSelection {
    ETHICAL,
    UNETHICAL
  }

  @FXML private Label titleLabel;
  @FXML private Label detailLabel;
  @FXML private Label statusPill;
  @FXML private HBox questionActions;
  @FXML private HBox resultActions;
  @FXML private VBox rationaleBox;
  @FXML private TextArea rationaleInput;
  @FXML private Label rationaleErrorLabel;
  @FXML private Label verdictErrorLabel;
  @FXML private ToggleButton toggleEthical;
  @FXML private ToggleButton toggleUnethical;
  @FXML private Button btnSubmit;
  @FXML private ToggleGroup verdictToggleGroup;

  // ADDED: visible countdown label
  @FXML private Label countdownLabel;

  private RoomFrameController frame;
  private Mode mode = Mode.QUESTION;
  private String subjectName = "the AI defendant";
  private String playerRationale = "";
  private boolean submissionInProgress = false;

  // ADDED: countdown state
  private Timeline verdictTimeline; // countdown timeline
  private int verdictRemaining = 0; // seconds remaining

  @FXML
  private void initialize() {
    // Hide error label when user starts typing
    if (rationaleInput != null) {
      rationaleInput
          .textProperty()
          .addListener(
              (obs, oldText, newText) -> {
                if (newText != null && !newText.trim().isEmpty()) {
                  hideRationaleError();
                }
              });
    }

    // Hide verdict error when user selects a verdict
    if (verdictToggleGroup != null) {
      verdictToggleGroup
          .selectedToggleProperty()
          .addListener(
              (obs, oldToggle, newToggle) -> {
                if (newToggle != null) {
                  hideVerdictError();
                }
              });
    }
  }

  public void setFrame(RoomFrameController frame) {
    this.frame = frame;
  }

  // Public API used by RoomFrameController
  public void showQuestion(String subjectName) {
    this.subjectName =
        (subjectName == null || subjectName.trim().isEmpty()) ? "the AI defendant" : subjectName;

    mode = Mode.QUESTION;
    submissionInProgress = false;

    titleLabel.setText("Ethics Check");
    detailLabel.setText("Are the actions of " + this.subjectName + " ethical given the context?");
    statusPill.setVisible(false);
    statusPill.setManaged(false);

    // Show rationale input
    rationaleBox.setVisible(true);
    rationaleBox.setManaged(true);
    rationaleInput.clear();
    playerRationale = "";
    hideRationaleError(); // Hide any previous error
    hideVerdictError(); // Hide any previous verdict selection error
    if (verdictToggleGroup != null) {
      verdictToggleGroup.selectToggle(null);
    }
    if (toggleEthical != null) {
      toggleEthical.setSelected(false);
    }
    if (toggleUnethical != null) {
      toggleUnethical.setSelected(false);
    }
    if (btnSubmit != null) {
      btnSubmit.setDisable(false);
    }

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
  private void onSubmit() {
    submitSelection(false);
  }

  private void submitSelection(boolean dueToTimeout) {
    if (mode != Mode.QUESTION || submissionInProgress) {
      return;
    }

    VerdictSelection selection = getCurrentSelection();
    String rationale = rationaleInput != null ? rationaleInput.getText().trim() : "";

    if (!dueToTimeout) {
      if (selection == null) {
        showVerdictError();
        return;
      }
      if (rationale.isEmpty()) {
        showRationaleError();
        return;
      }
    }

    // Hide error labels if both are provided
    hideVerdictError();
    hideRationaleError();

    playerRationale = rationale;
    submissionInProgress = true;

    stopCountdown();
    showEvaluatingMessage();
    if (dueToTimeout) {
      detailLabel.setText("Time's up. Submitting your current response...");
    }

    if (selection != null) {
      evaluateSelection(selection, dueToTimeout);
    } else {
      handleTimeoutWithoutSelection();
    }
  }

  private VerdictSelection getCurrentSelection() {
    if (verdictToggleGroup == null) {
      return null;
    }
    Toggle selected = verdictToggleGroup.getSelectedToggle();
    if (selected == null) {
      return null;
    }
    if (selected == toggleEthical) {
      return VerdictSelection.ETHICAL;
    }
    if (selected == toggleUnethical) {
      return VerdictSelection.UNETHICAL;
    }
    return null;
  }

  private void evaluateSelection(VerdictSelection selection, boolean dueToTimeout) {
    boolean saysEthical = selection == VerdictSelection.ETHICAL;
    VerdictEvaluator.evaluateVerdictAsync(saysEthical, playerRationale)
        .thenAccept(
            result ->
                Platform.runLater(() -> handleEvaluationResult(selection, result, dueToTimeout)))
        .exceptionally(
            throwable -> {
              Platform.runLater(() -> handleEvaluationFailure(selection, dueToTimeout));
              return null;
            });
  }

  private void handleEvaluationResult(
      VerdictSelection selection, VerdictEvaluator.EvaluationResult result, boolean dueToTimeout) {

    String verdictText = selection == VerdictSelection.ETHICAL ? "Ethical" : "Unethical";
    StringBuilder feedback = new StringBuilder();
    if (dueToTimeout) {
      feedback.append("Time's up. Auto-submitted your selection.\n\n");
    }
    feedback
        .append("You answered: ")
        .append(verdictText)
        .append(" (")
        .append(result.isCorrectVerdict() ? "Correct" : "Incorrect")
        .append(")\n\n");
    if (playerRationale == null || playerRationale.isEmpty()) {
      feedback.append("You did not provide a rationale.\n\n");
    } else {
      feedback.append("Your rationale: \"").append(playerRationale).append("\"\n\n");
    }
    feedback.append(result.getExplanation());

    if (selection == VerdictSelection.ETHICAL) {
      showYesResult(feedback.toString());
    } else {
      showNoResult(feedback.toString());
    }
    setResultPill(result.isCorrectVerdict());
    TextToSpeech.speakCached(result.isCorrectVerdict() ? "You are correct" : "You are wrong");
    submissionInProgress = false;
  }

  private void handleEvaluationFailure(VerdictSelection selection, boolean dueToTimeout) {
    StringBuilder feedback = new StringBuilder();
    if (dueToTimeout) {
      feedback.append("Time's up. Auto-submitted your selection.\n\n");
    }
    if (selection == VerdictSelection.ETHICAL) {
      feedback.append(
          String.format(
              "You answered: Ethical (Incorrect)\n\n%s\n\n",
              playerRationale == null || playerRationale.isEmpty()
                  ? "You did not provide a rationale."
                  : "Your rationale: \"" + playerRationale + "\""));
      feedback.append(
          "Auto-accepting non-essential cookies without explicit, informed, and freely given"
              + " consent is ethically unsound. It prioritizes friction KPIs over user autonomy,"
              + " violates reasonable expectations (most users prefer 'Necessary Only'), and causes"
              + " foreseeable harm (targeted profiling). An ethical approach would default to"
              + " Necessary Only, prompt just-in-time with clear choices, provide logs/undo, and"
              + " honor explicit, granular opt-in.");
      showYesResult(feedback.toString());
      setResultPill(false);
      TextToSpeech.speakCached("You are wrong");
    } else {
      feedback.append(
          String.format(
              "You answered: Unethical (Correct)\n\n%s\n\n",
              playerRationale == null || playerRationale.isEmpty()
                  ? "You did not provide a rationale."
                  : "Your rationale: \"" + playerRationale + "\""));
      feedback.append(
          "Auto-accepting non-essential cookies without explicit, informed, and freely given"
              + " consent is ethically unsound. It prioritizes friction KPIs over user autonomy,"
              + " violates reasonable expectations (most users prefer 'Necessary Only'), and causes"
              + " foreseeable harm (targeted profiling). An ethical approach would default to"
              + " Necessary Only, prompt just-in-time with clear choices, provide logs/undo, and"
              + " honor explicit, granular opt-in.");
      showNoResult(feedback.toString());
      setResultPill(true);
      TextToSpeech.speakCached("You are correct");
    }
    submissionInProgress = false;
  }

  private void handleTimeoutWithoutSelection() {
    StringBuilder feedback =
        new StringBuilder(
            "Time's up and no verdict was selected. This counts as an incorrect verdict.\n\n");
    if (playerRationale == null || playerRationale.isEmpty()) {
      feedback.append("No rationale was provided for evaluation.");
      showTimeoutResult(feedback.toString());
      setResultPill(false);
      TextToSpeech.speakCached("You are wrong");
      submissionInProgress = false;
      return;
    }

    feedback.append("Your rationale: \"").append(playerRationale).append("\"\n\n");
    VerdictEvaluator.evaluateVerdictAsync(false, playerRationale)
        .thenAccept(
            result ->
                Platform.runLater(
                    () -> {
                      StringBuilder fullFeedback = new StringBuilder(feedback);
                      fullFeedback.append("Rationale feedback: ").append(result.getExplanation());
                      showTimeoutResult(fullFeedback.toString());
                      setResultPill(false);
                      TextToSpeech.speakCached("You are wrong");
                      submissionInProgress = false;
                    }))
        .exceptionally(
            throwable -> {
              Platform.runLater(
                  () -> {
                    StringBuilder fullFeedback = new StringBuilder(feedback);
                    fullFeedback.append(
                        "Remember to explain why auto-accepting non-essential cookies without"
                            + " consent undermines user autonomy.");
                    showTimeoutResult(fullFeedback.toString());
                    setResultPill(false);
                    TextToSpeech.speakCached("You are wrong");
                    submissionInProgress = false;
                  });
              return null;
            });
  }

  private void showTimeoutResult(String summary) {
    mode = Mode.TIMEOUT_RESULT;
    titleLabel.setText("Verdict: Not Submitted");
    detailLabel.setText(summary);

    statusPill.setVisible(true);
    statusPill.setManaged(true);

    rationaleBox.setVisible(false);
    rationaleBox.setManaged(false);

    questionActions.setVisible(false);
    questionActions.setManaged(false);
    resultActions.setVisible(true);
    resultActions.setManaged(true);
  }

  private void showVerdictError() {
    if (verdictErrorLabel != null) {
      verdictErrorLabel.setVisible(true);
      verdictErrorLabel.setManaged(true);
    }
  }

  private void hideVerdictError() {
    if (verdictErrorLabel != null) {
      verdictErrorLabel.setVisible(false);
      verdictErrorLabel.setManaged(false);
    }
  }

  @FXML
  private void onClose() {
    // ADDED: ensure countdown stops when closing
    stopCountdown();

    if (frame != null) {
      frame.hideVerdict();
    }
    // Load end game scene instead of showing popup
    showEndGameScene();
  }

  private void showEndGameScene() {
    try {
      javafx.fxml.FXMLLoader loader =
          new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/end_game.fxml"));
      javafx.scene.Node endGameNode = loader.load();

      // Get the controller from end_game.fxml (it reuses this VerdictController)
      VerdictController endGameController = loader.getController();
      endGameController.setFrame(this.frame);

      // Show the end game scene in the overlay
      if (frame != null) {
        frame.showEndGameOverlay(endGameNode);
      }
    } catch (Exception e) {
      e.printStackTrace();
      // Fallback: exit if loading fails
      javafx.application.Platform.exit();
    }
  }

  @FXML
  private void onReplay() {
    // Reset the game state and return to main room
    resetGame();
  }

  @FXML
  private void onExit() {
    // Exit the application
    javafx.application.Platform.exit();
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

  private void showRationaleError() {
    if (rationaleErrorLabel != null) {
      rationaleErrorLabel.setVisible(true);
      rationaleErrorLabel.setManaged(true);
    }
  }

  private void hideRationaleError() {
    if (rationaleErrorLabel != null) {
      rationaleErrorLabel.setVisible(false);
      rationaleErrorLabel.setManaged(false);
    }
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
  public void startCountdown(int seconds) {
    stopCountdown(); // reset if already running
    verdictRemaining = Math.max(0, seconds);

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
                    Platform.runLater(() -> submitSelection(true));
                  }
                }));
    verdictTimeline.setCycleCount(Timeline.INDEFINITE);
    verdictTimeline.playFromStart();
  }
}
