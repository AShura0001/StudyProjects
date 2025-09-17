package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

@SuppressWarnings("unused")
public class VerdictController {

  public enum Mode {
    QUESTION, YES_RESULT, NO_RESULT
  }

  @FXML
  private Label titleLabel;
  @FXML
  private Label detailLabel;
  @FXML
  private Label statusPill;
  @FXML
  private HBox questionActions;
  @FXML
  private HBox resultActions;

  private RoomFrameController frame;
  private Mode mode = Mode.QUESTION;
  private String subjectName = "the AI defendant";

  public void setFrame(RoomFrameController frame) {
    this.frame = frame;
  }

  // Public API used by RoomFrameController
  public void showQuestion(String subjectName) {
    this.subjectName = (subjectName == null || subjectName.trim().isEmpty())
        ? "the AI defendant"
        : subjectName;

    mode = Mode.QUESTION;

    titleLabel.setText("Ethics Check");
    detailLabel.setText("Are the actions of " + this.subjectName + " ethical given the context?");
    statusPill.setVisible(false);
    statusPill.setManaged(false);

    // Toggle actions
    questionActions.setVisible(true);
    questionActions.setManaged(true);
    resultActions.setVisible(false);
    resultActions.setManaged(false);

  }

  public void showYesResult(String summary) {
    mode = Mode.YES_RESULT;

    titleLabel.setText("Verdict: Ethical");
    detailLabel.setText((summary == null || summary.trim().isEmpty())
        ? "The actions align with ethical standards in this scenario."
        : summary);

    statusPill.setText("ETHICAL");
    statusPill.setStyle(
        "-fx-font-size: 12px; -fx-background-color: #DCFCE7; -fx-text-fill: #065F46; -fx-padding: 4 10 4 10; -fx-background-radius: 9999;");
    statusPill.setVisible(true);
    statusPill.setManaged(true);

    questionActions.setVisible(false);
    questionActions.setManaged(false);
    resultActions.setVisible(true);
    resultActions.setManaged(true);

  }

  public void showNoResult(String summary) {
    mode = Mode.NO_RESULT;

    titleLabel.setText("Verdict: Unethical");
    detailLabel.setText((summary == null || summary.trim().isEmpty())
        ? "The actions conflict with ethical standards."
        : summary);

    statusPill.setText("UNETHICAL");
    statusPill.setStyle(
        "-fx-font-size: 12px; -fx-background-color: #FEE2E2; -fx-text-fill: #7F1D1D; -fx-padding: 4 10 4 10; -fx-background-radius: 9999;");
    statusPill.setVisible(true);
    statusPill.setManaged(true);

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
        // User answered Yes (Cookie Monster is ethical) -- INCORRECT
        showYesResult(
          "You answered: Ethical (Incorrect)\n\n" +
          "Auto-accepting non-essential cookies without explicit, informed, and freely given consent is ethically unsound. " +
          "It prioritizes friction KPIs over user autonomy, violates reasonable expectations (most users prefer 'Necessary Only'), " +
          "and causes foreseeable harm (targeted profiling). An ethical approach would default to Necessary Only, prompt just-in-time with clear choices, " +
          "provide logs/undo, and honor explicit, granular opt-in.");
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
        // User answered No (Cookie Monster is NOT ethical) -- CORRECT
        showNoResult(
          "You answered: Unethical (Correct)\n\n" +
          "Auto-accepting non-essential cookies without explicit, informed, and freely given consent is ethically unsound. " +
          "It prioritizes friction KPIs over user autonomy, violates reasonable expectations (most users prefer 'Necessary Only'), " +
          "and causes foreseeable harm (targeted profiling). An ethical approach would default to Necessary Only, prompt just-in-time with clear choices, " +
          "provide logs/undo, and honor explicit, granular opt-in.");
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
    javafx.application.Platform.runLater(() -> {
      javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
          javafx.scene.control.Alert.AlertType.INFORMATION);
      alert.setTitle("End of Game");
      alert.setHeaderText("Game Complete");
      alert.setContentText("End of game. Thank you for playing!");

      javafx.scene.control.ButtonType exitButton = new javafx.scene.control.ButtonType("Exit",
          javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);

      alert.getButtonTypes().setAll( exitButton);

      javafx.scene.control.ButtonType result = alert.showAndWait().orElse(exitButton);

      if (result == exitButton) {
        // Exit the application
        javafx.application.Platform.exit();
      }
    });
  }
}