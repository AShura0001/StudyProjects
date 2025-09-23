package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.Set;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.GameStateContext;
import nz.ac.auckland.se206.controllers.ChatController.Participant;

public class RoomFrameController {
  // Track which flashbacks have been viewed (1-based index)
  private boolean[] flashbackViewed = new boolean[] {false, false, false};

  // FXML refs
  @FXML private StackPane contentHost;
  @FXML private StackPane overlayRoot;
  @FXML private Button btnStart;
  @FXML private Button btnEndGame;
  @FXML private Label timerLabel;
  @FXML private VBox chatBox;

  // Timer
  private static final int START_SECONDS = 120; // adjust as needed
  private int remaining = START_SECONDS;
  private Timeline timeline;

  @FXML
  public void initialize() {
    contentHost.getChildren().setAll(placeholderNode("Waiting to start..."));
    contentHost.setDisable(true);
    updateTimerLabel(remaining);
    setupTimer();

    if (overlayRoot != null) {
      overlayRoot.setVisible(false);
      overlayRoot.setManaged(false);
    }

    // Manually load chat.fxml to get the controller instance and inject it
    try {
      FXMLLoader chatLoader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
      VBox chatPanel = chatLoader.load();
      ChatController chatController = chatLoader.getController();
      nz.ac.auckland.se206.App.getGameStateContext().setChatController(chatController);
      // Optionally replace the included chatBox with the loaded one
      if (chatBox != null && chatPanel != null) {
        chatBox.getChildren().setAll(chatPanel.getChildren());
      }
    } catch (Exception e) {
      System.err.println("Failed to inject ChatController: " + e.getMessage());
    }
  }

  @FXML
  private void onStartGame() {
    contentHost.setDisable(false);
    resetTimer();
    startTimer();
    btnStart.setVisible(false);
    btnStart.setManaged(false);

    // Load the room into the center when starting
    showRoom();
  }

  // Load room.fxml and wire parent reference
  public void showRoom() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/room.fxml"));
      Parent node = loader.load();

      RoomController room = loader.getController();
      room.setParent(this); // allow room to request flashbacks

      contentHost.getChildren().setAll(node);

    } catch (IOException ex) {
      contentHost.getChildren().setAll(placeholderNode("Failed to load room"));
      ex.printStackTrace();
    }
  }

  // Swap in a flashback by index (1..3)
  public void showFlashback(int index) {
    // Allow any valid flashback index
    if (index < 1 || index > 3) {
      showRoom();
      return;
    }

    // Mark as viewed (for tracking purposes, but don't prevent re-viewing)
    if (index >= 1 && index <= 3) {
      flashbackViewed[index - 1] = true;
    }

    String path = "/fxml/flashback" + index + ".fxml";
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
      Parent node = loader.load();

      // Give flashback controller a back reference
      Object ctl = loader.getController();
      if (ctl instanceof FlashbackController) {
        ((FlashbackController) ctl).setParent(this);

        // Pass GameStateContext to FlashbackController for chat integration
        nz.ac.auckland.se206.GameStateContext gameState =
            nz.ac.auckland.se206.App.getGameStateContext();
        if (gameState != null) {
          ((FlashbackController) ctl).setGameStateContext(gameState);
        }
      }
      // Switch chat participant in ChatController
      nz.ac.auckland.se206.controllers.ChatController.Participant chatParticipant;
      switch (index) {
        case 1:
          chatParticipant = nz.ac.auckland.se206.controllers.ChatController.Participant.DEFENDANT;
          break;
        case 2:
          chatParticipant = nz.ac.auckland.se206.controllers.ChatController.Participant.AI_WITNESS;
          break;
        case 3:
          chatParticipant =
              nz.ac.auckland.se206.controllers.ChatController.Participant.HUMAN_WITNESS;
          break;
        default:
          chatParticipant = nz.ac.auckland.se206.controllers.ChatController.Participant.DEFENDANT;
          break;
      }
      // Find ChatController and switch participant
      // This assumes you have a reference to ChatController, e.g. via GameStateContext
      nz.ac.auckland.se206.GameStateContext gameState =
          nz.ac.auckland.se206.App.getGameStateContext();
      if (gameState != null) {
        gameState.focusChatForParticipant(chatParticipant);
      }

      contentHost.getChildren().setAll(node);

    } catch (IOException ex) {
      contentHost.getChildren().setAll(placeholderNode("Failed to load flashback"));
      ex.printStackTrace();
    }
  }

  // Allow flashbacks to navigate back
  public void backToRoom() {
    showRoom();
  }

  // NEW: End Game button handler
  @FXML
  private void onEndGame() {
    // Check if all participants have been contacted before allowing verdict
    GameStateContext gameState = App.getGameStateContext();
    if (gameState != null && !gameState.hasContactedAllParticipants()) {
      showIncompleteContactAlert(gameState);
      return;
    }

    // Stop timer and open verdict overlay
    if (timeline != null) timeline.stop();
    showVerdictQuestion("the AI defendant");
  }

  // ----------------- Timer -----------------

  private void setupTimer() {
    if (timeline != null) timeline.stop();
    timeline =
        new Timeline(
            new KeyFrame(
                Duration.seconds(1),
                e -> {
                  remaining = Math.max(0, remaining - 1);
                  updateTimerLabel(remaining);
                  if (remaining == 0) {
                    timeline.stop();
                    // Check if all participants contacted before showing verdict
                    GameStateContext gameState = App.getGameStateContext();
                    if (gameState != null && !gameState.hasContactedAllParticipants()) {
                      showGameOverAlert(gameState);
                    } else {
                      // Show overlay on time up
                      showOverlay();
                      showVerdictQuestion("the AI defendant");
                    }
                  }
                }));
    timeline.setCycleCount(Timeline.INDEFINITE);
  }

  private void startTimer() {
    if (timeline != null && timeline.getStatus() != Timeline.Status.RUNNING) {
      timeline.play();
    }
  }

  private void resetTimer() {
    if (timeline != null) timeline.stop();
    remaining = START_SECONDS;
    updateTimerLabel(remaining);
    setupTimer();
  }

  private void updateTimerLabel(int seconds) {
    if (timerLabel == null) return;
    int mm = seconds / 60;
    int ss = seconds % 60;
    timerLabel.setText(String.format("%02d:%02d", mm, ss));
  }

  // ----------------- Center content loading -----------------

  @SuppressWarnings("unused")
  private void setCenterFromFxml(String resourcePath) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
      Parent node = loader.load();
      contentHost.getChildren().setAll(node);
    } catch (IOException ex) {
      contentHost.getChildren().setAll(placeholderNode("Failed to load content"));
      ex.printStackTrace();
    }
  }

  private Node placeholderNode(String text) {
    Label lbl = new Label(text);
    lbl.setStyle("-fx-text-fill: gray;");
    return lbl;
  }

  // ----------------- Overlay controls -----------------

  public void showOverlay() {
    overlayRoot.setVisible(true);
    overlayRoot.setManaged(true);
  }

  public void hideOverlay() {
    overlayRoot.setVisible(false);
    overlayRoot.setManaged(false);
  }

  // Verdict helpers

  // Load verdict.fxml into overlayRoot and show question
  public void showVerdictQuestion(String subjectName) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/verdict.fxml"));
      Node verdictNode = loader.load();

      VerdictController controller = loader.getController();
      controller.setFrame(this);
      controller.showQuestion(subjectName);

      overlayRoot.getChildren().setAll(verdictNode);
      showOverlay();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  // Called by the verdict overlay to close itself safely
  public void hideVerdict() {
    hideOverlay();
    if (overlayRoot != null) {
      overlayRoot.getChildren().clear();
    }
  }

  // Helper methods for participant contact validation

  private void showIncompleteContactAlert(GameStateContext gameState) {
    Set<Participant> uncontacted = gameState.getUncontactedParticipants();
    StringBuilder message =
        new StringBuilder(
            "You must chat with all participants before making a verdict!\n\n"
                + "You still need to contact:\n");

    for (Participant p : uncontacted) {
      message.append("• ").append(getParticipantDisplayName(p)).append("\n");
    }

    message.append("\nPlease click on the participants to chat with them.");

    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle("Incomplete Investigation");
    alert.setHeaderText("Cannot Make Verdict Yet");
    alert.setContentText(message.toString());
    alert.showAndWait();
  }

  private void showGameOverAlert(GameStateContext gameState) {
    Set<Participant> uncontacted = gameState.getUncontactedParticipants();
    StringBuilder message =
        new StringBuilder("Time's up! You did not chat with all participants.\n\nYou missed:\n");

    for (Participant p : uncontacted) {
      message.append("• ").append(getParticipantDisplayName(p)).append("\n");
    }

    message.append("\nGame Over - Investigation incomplete!");

    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Game Over");
    alert.setHeaderText("Investigation Incomplete");
    alert.setContentText(message.toString());
    alert.showAndWait();

    // Exit the application after game over
    javafx.application.Platform.exit();
  }

  private String getParticipantDisplayName(Participant participant) {
    switch (participant) {
      case DEFENDANT:
        return "Cookie Monster (Defendant)";
      case AI_WITNESS:
        return "Sentri-Guard (AI Witness)";
      case HUMAN_WITNESS:
        return "Casey (Human Witness)";
      default:
        return "Unknown Participant";
    }
  }

  // ----------------- Cleanup -----------------

  public void shutdown() {
    if (timeline != null) timeline.stop();
  }
}
