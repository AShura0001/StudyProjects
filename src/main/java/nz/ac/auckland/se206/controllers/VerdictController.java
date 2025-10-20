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
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.GameStateContext;
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
  @FXML private TextFlow detailTextFlow;
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
  @FXML private VBox hintsBox;
  @FXML private TextArea hintsDisplay;

  private static final String DETAIL_TEXT_STYLE = "-fx-fill: #333333; -fx-font-size: 18;";
  private static final String BASE_FALLBACK_FEEDBACK =
      "Auto-accepting non-essential cookies without explicit, informed, and freely given consent "
          + "is ethically unsound. It prioritizes friction KPIs over user autonomy, violates "
          + "reasonable expectations (most users prefer \"Necessary Only\"), and causes foreseeable "
          + "harm (targeted profiling). An ethical approach would default to Necessary Only, prompt "
          + "just-in-time with clear choices, provide logs/undo, and honor explicit, granular opt-in.";

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
    setDetailText("Are the actions of " + this.subjectName + " ethical given the context?");
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
    
    // Populate hints display
    populateHints();
  }

  public void showYesResult(String summary) {
    mode = Mode.YES_RESULT;

    // ADDED: stop countdown once a result is displayed
    stopCountdown();

    titleLabel.setText("Verdict: Ethical");
    setDetailText(
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

  private void setDetailText(String text) {
    if (detailTextFlow == null) {
      return;
    }

    detailTextFlow.getChildren().clear();

    if (text == null || text.isEmpty()) {
      return;
    }

    Text content = new Text(text);
    content.setStyle(DETAIL_TEXT_STYLE);
    content.wrappingWidthProperty().bind(detailTextFlow.widthProperty());
    detailTextFlow.getChildren().add(content);
  }

  private boolean hasPlayerRationale() {
    return playerRationale != null && !playerRationale.trim().isEmpty();
  }

  private String buildOutcomeText(boolean correctVerdict, boolean correctRationale) {
    if (correctVerdict && correctRationale) {
      return "Correct verdict with correct rationale";
    } else if (correctVerdict) {
      return "Correct verdict with incorrect rationale";
    }
    return "Incorrect verdict";
  }

  private String buildFeedbackSummary(
      String verdictText,
      boolean correctVerdict,
      boolean correctRationale,
      String instructorFeedback,
      boolean dueToTimeout) {

    StringBuilder feedback = new StringBuilder();

    if (dueToTimeout) {
      feedback.append("Time's up. Auto-submitted your selection.\n\n");
    }

    feedback
        .append("You answered: ")
        .append(verdictText)
        .append(" (")
        .append(correctVerdict ? "Correct" : "Incorrect")
        .append(")\n");

    if (hasPlayerRationale()) {
      feedback
          .append("Rationale evaluation: ")
          .append(correctRationale ? "Correct" : "Needs improvement")
          .append("\n")
          .append("Your rationale: \"")
          .append(playerRationale)
          .append("\"\n");
    } else {
      feedback.append("Rationale evaluation: Not provided.\n");
    }

    feedback
        .append("\nOutcome summary: ")
        .append(buildOutcomeText(correctVerdict, correctRationale))
        .append("\n\n")
        .append("Rationale feedback:\n")
        .append(instructorFeedback);

    return feedback.toString();
  }

  public void showNoResult(String summary) {
    mode = Mode.NO_RESULT;

    // ADDED: stop countdown once a result is displayed
    stopCountdown();

    titleLabel.setText("Verdict: Unethical");
    setDetailText(
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
      setDetailText("Time's up. Submitting your current response...");
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
    String feedback =
        buildFeedbackSummary(
            verdictText,
            result.isCorrectVerdict(),
            result.isCorrectRationale(),
            result.getFeedback(),
            dueToTimeout);

    if (selection == VerdictSelection.ETHICAL) {
      showYesResult(feedback);
    } else {
      showNoResult(feedback);
    }
    setResultPill(result.isCorrectVerdict());
    TextToSpeech.speakCached(result.isCorrectVerdict() ? "You are correct" : "You are wrong");
    submissionInProgress = false;
  }

  private void handleEvaluationFailure(VerdictSelection selection, boolean dueToTimeout) {
    String verdictText = selection == VerdictSelection.ETHICAL ? "Ethical" : "Unethical";
    boolean correctVerdict = selection == VerdictSelection.UNETHICAL;
    boolean correctRationale = correctVerdict && hasPlayerRationale() && playerRationale.length() > 20;

    String feedback =
        buildFeedbackSummary(
            verdictText,
            correctVerdict,
            correctRationale,
            "Fallback review: " + BASE_FALLBACK_FEEDBACK,
            dueToTimeout);

    if (selection == VerdictSelection.ETHICAL) {
      showYesResult(feedback);
      setResultPill(false);
      TextToSpeech.speakCached("You are wrong");
    } else {
      showNoResult(feedback);
      setResultPill(true);
      TextToSpeech.speakCached("You are correct");
    }
    submissionInProgress = false;
  }

  private void handleTimeoutWithoutSelection() {
    String verdictText = "No verdict selected";

    if (!hasPlayerRationale()) {
      String feedback =
          buildFeedbackSummary(
              verdictText,
              false,
              false,
              "Without a submitted rationale we cannot evaluate your reasoning. Explain your stance before the timer expires.",
              true);
      showTimeoutResult(feedback);
      setResultPill(false);
      TextToSpeech.speakCached("You are wrong");
      submissionInProgress = false;
      return;
    }

    VerdictEvaluator.evaluateVerdictAsync(false, playerRationale)
        .thenAccept(
            result ->
                Platform.runLater(
                    () -> {
                      String feedback =
                          buildFeedbackSummary(
                              verdictText,
                              false,
                              result.isCorrectRationale(),
                              "Rationale check only: " + result.getFeedback(),
                              true);
                      showTimeoutResult(feedback);
                      setResultPill(false);
                      TextToSpeech.speakCached("You are wrong");
                      submissionInProgress = false;
                    }))
        .exceptionally(
            throwable -> {
              Platform.runLater(
                  () -> {
                    String feedback =
                        buildFeedbackSummary(
                            verdictText,
                            false,
                            false,
                            "Fallback review: " + BASE_FALLBACK_FEEDBACK,
                            true);
                    showTimeoutResult(feedback);
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
    setDetailText(summary);

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
    setDetailText("Please wait while we analyze your verdict and rationale using AI.");

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
  
  // ======== Hints Display ========
  
  /** Populates the hints display with all discovered hints */
  private void populateHints() {
    if (hintsDisplay == null || hintsBox == null) {
      return;
    }
    
    GameStateContext gameState = App.getGameStateContext();
    if (gameState == null) {
      hintsBox.setVisible(false);
      hintsBox.setManaged(false);
      return;
    }
    
    java.util.List<String> hints = gameState.getDiscoveredHints();
    if (hints.isEmpty()) {
      hintsBox.setVisible(false);
      hintsBox.setManaged(false);
      return;
    }
    
    // Build hints text
    StringBuilder hintsText = new StringBuilder();
    for (String hint : hints) {
      hintsText.append("• ").append(hint).append("\n\n");
    }
    
    hintsDisplay.setText(hintsText.toString().trim());
    hintsBox.setVisible(true);
    hintsBox.setManaged(true);
  }
}


