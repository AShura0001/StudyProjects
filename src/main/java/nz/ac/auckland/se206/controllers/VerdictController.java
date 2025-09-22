package nz.ac.auckland.se206.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import nz.ac.auckland.se206.prompts.VerdictEvaluator;

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

  private RoomFrameController frame;
  private Mode mode = Mode.QUESTION;
  private String subjectName = "the AI defendant";
  private String playerRationale = "";

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
  }

  public void showYesResult(String summary) {
    mode = Mode.YES_RESULT;

    titleLabel.setText("Verdict: Ethical");
    detailLabel.setText(
        (summary == null || summary.trim().isEmpty())
            ? "The actions align with ethical standards in this scenario."
            : summary);

    statusPill.setText("ETHICAL");
    statusPill.setStyle(
        "-fx-font-size: 12px; -fx-background-color: #DCFCE7; -fx-text-fill: #065F46; -fx-padding: 4"
            + " 10 4 10; -fx-background-radius: 9999;");
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

    titleLabel.setText("Verdict: Unethical");
    detailLabel.setText(
        (summary == null || summary.trim().isEmpty())
            ? "The actions conflict with ethical standards."
            : summary);

    statusPill.setText("UNETHICAL");
    statusPill.setStyle(
        "-fx-font-size: 12px; -fx-background-color: #FEE2E2; -fx-text-fill: #7F1D1D; -fx-padding: 4"
            + " 10 4 10; -fx-background-radius: 9999;");
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
    // “Yes” when in QUESTION -> showYesResult; in results, treat as primary action
    switch (mode) {
      case QUESTION:
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
    // “No” when in QUESTION -> showNoResult; in results, treat as secondary action
    // (Close)
    switch (mode) {
      case QUESTION:
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
    if (frame != null) {
      frame.hideVerdict();
    }
    // Show end of game popup with Retry and Exit buttons
    javafx.application.Platform.runLater(
        () -> {
          javafx.scene.control.Alert alert =
              new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
          alert.setTitle("End of Game");
          alert.setHeaderText("Game Complete");
          alert.setContentText("End of game. Thank you for playing!");

          javafx.scene.control.ButtonType exitButton =
              new javafx.scene.control.ButtonType(
                  "Exit", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);

          alert.getButtonTypes().setAll(exitButton);

          javafx.scene.control.ButtonType result = alert.showAndWait().orElse(exitButton);

          if (result == exitButton) {
            // Exit the application
            javafx.application.Platform.exit();
          }
        });
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
}
