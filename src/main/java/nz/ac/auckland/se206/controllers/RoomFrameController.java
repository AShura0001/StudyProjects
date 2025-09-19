package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

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
    // Only allow flashback if not already viewed
    if (index < 1 || index > 3) {
      showRoom();
      return;
    }
    if (flashbackViewed[index - 1]) {
      // Already viewed, do nothing
      return;
    }
    // Mark as viewed
    flashbackViewed[index - 1] = true;
    String path = "/fxml/flashback" + index + ".fxml";
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
      Parent node = loader.load();

      // Give flashback controller a back reference
      Object ctl = loader.getController();
      if (ctl instanceof FlashbackController) {
        ((FlashbackController) ctl).setParent(this);
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
    // Stop timer and open verdict overlay immediately
    if (timeline != null) timeline.stop();
    showVerdictQuestion("the AI defendant"); // pass your subject/context if needed
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
                    // Show overlay on time up
                    showOverlay();
                    showVerdictQuestion("the AI defendant"); // pass your subject/context if needed
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

  // ----------------- Cleanup -----------------

  public void shutdown() {
    if (timeline != null) timeline.stop();
  }
}
