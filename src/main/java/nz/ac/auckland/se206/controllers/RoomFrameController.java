package nz.ac.auckland.se206.controllers;

import java.io.IOException;
// ADDED
import java.lang.reflect.Method;
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
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.GameStateContext;
import nz.ac.auckland.se206.controllers.ChatController.Participant;

public class RoomFrameController {
  private boolean[] flashbackViewed = new boolean[] {false, false, false};

  @FXML private StackPane contentHost;
  @FXML private StackPane overlayRoot;
  @FXML private Button btnStart;
  @FXML private Button btnEndGame;
  @FXML private Label timerLabel;
  @FXML private VBox chatBox;
  @FXML private ImageView roomImage;
  @FXML private StackPane imageHost;
  @FXML private Button hotspotA;
  @FXML private Button hotspotB;
  @FXML private Button hotspotC;

  // Store original room content for restoration
  private Node originalRoomContent;

  // Timer
  // CHANGED: 120 -> 300 (5 minutes)
  private static final int START_SECONDS = 300; // ADDED: set round to 5 minutes
  private int remaining = START_SECONDS;
  private Timeline timeline;

  @FXML
  public void initialize() {
    initializeInitialState();

    updateTimerLabel(remaining);
    setupTimer();

    if (overlayRoot != null) {
      overlayRoot.setVisible(false);
      overlayRoot.setManaged(false);
    }

    try {
      loadAndInjectChatController();
    } catch (IOException e) {
      System.err.println("Failed to load chat controller: " + e.getMessage());
    }
  }

  private void initializeInitialState() {

    if (contentHost.getChildren().size() > 0) {
      originalRoomContent = contentHost.getChildren().get(0);
    }

    contentHost.setDisable(true);
    contentHost.setVisible(false);
    contentHost.setManaged(false);

    btnEndGame.setVisible(false);
    btnEndGame.setManaged(false);

    btnStart.setVisible(true);
    btnStart.setManaged(true);
    timerLabel.setVisible(true);
    timerLabel.setManaged(true);
  }

  /**
   * Loads the chat.fxml and injects the ChatController into the GameStateContext. This is used both
   * during initialization and when resetting the game.
   */
  private void loadAndInjectChatController() throws IOException {
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
      throw new IOException("Failed to load chat controller", e);
    }
  }

  @FXML
  private void onStartGame() {
    // Show and enable all game content
    contentHost.setVisible(true);
    contentHost.setManaged(true);
    contentHost.setDisable(false);

    btnEndGame.setVisible(true);
    btnEndGame.setManaged(true);

    btnStart.setVisible(false);
    btnStart.setManaged(false);

    resetTimer();
    startTimer();
  }

  public void showRoom() {
    // Restore the original room content after returning from a flashback
    if (originalRoomContent != null) {
      contentHost.getChildren().setAll(originalRoomContent);
    } else {
      System.err.println("No original room content stored - this shouldn't happen");
      contentHost.getChildren().setAll(placeholderNode("Failed to restore room"));
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

  // Hotspot handlers (wired from FXML onAction)
  @FXML
  private void onHotspotA() {
    System.out.println("DEBUG: Hotspot A clicked - Human Witness");
    // Navigate to flashback
    showFlashback(1);
    // Hotspot A is Human Witness
    var gameState = nz.ac.auckland.se206.App.getGameStateContext();
    if (gameState != null) {
      gameState.focusChatForParticipant(
          nz.ac.auckland.se206.controllers.ChatController.Participant.HUMAN_WITNESS);
      gameState.requestOpeningMessage(
          nz.ac.auckland.se206.controllers.ChatController.Participant.HUMAN_WITNESS);
    }
  }

  @FXML
  private void onHotspotB() {
    System.out.println("DEBUG: Hotspot B clicked - AI Witness");
    showFlashback(2);
    var gameState = nz.ac.auckland.se206.App.getGameStateContext();
    if (gameState != null) {
      gameState.focusChatForParticipant(
          nz.ac.auckland.se206.controllers.ChatController.Participant.AI_WITNESS);
      gameState.requestOpeningMessage(
          nz.ac.auckland.se206.controllers.ChatController.Participant.AI_WITNESS);
    }
  }

  @FXML
  private void onHotspotC() {
    System.out.println("DEBUG: Hotspot C clicked - Defendant");
    showFlashback(3);
    // Hotspot C is Defendant
    var gameState = nz.ac.auckland.se206.App.getGameStateContext();
    if (gameState != null) {
      gameState.focusChatForParticipant(
          nz.ac.auckland.se206.controllers.ChatController.Participant.DEFENDANT);
      gameState.requestOpeningMessage(
          nz.ac.auckland.se206.controllers.ChatController.Participant.DEFENDANT);
    }
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
    // ADDED: lock interactions + verdict with 60s countdown
    lockRoomInteractions(); // ADDED
    showVerdictWithCountdown(); // ADDED
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
                      // ADDED: lock and open verdict with 60s countdown
                      lockRoomInteractions(); // ADDED
                      showVerdictWithCountdown(); // ADDED
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

  // ADDED: Lock room/chat interactions once verdict stage begins
  private void lockRoomInteractions() {
    if (contentHost != null) {
      contentHost.setDisable(true);
    }
    // If you need to disable chat inputs explicitly, do it via GameStateContext -> ChatController.
  }

  // ADDED: Open verdict overlay AND attempt to start a 60s countdown (if controller supports it)
  private void showVerdictWithCountdown() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/verdict.fxml"));
      Node verdictNode = loader.load();

      VerdictController controller = loader.getController();
      controller.setFrame(this);
      controller.showQuestion("the AI defendant");

      overlayRoot.getChildren().setAll(verdictNode);
      showOverlay();

      // Try to call controller.startCountdown(60, Runnable) if it exists.
      // This keeps this file self-contained; if you haven't added startCountdown yet,
      // nothing breaks—the verdict just won’t auto-timeout.
      try {
        Method m = controller.getClass().getMethod("startCountdown", int.class, Runnable.class);
        m.invoke(controller, 60, (Runnable) this::onVerdictTimeout);
      } catch (NoSuchMethodException nsme) {
        // Method not present yet: safe to ignore; you can add it later in VerdictController.
      } catch (Exception reflectionError) {
        reflectionError.printStackTrace();
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  // ADDED: What to do when verdict 60s expires (if startCountdown calls back)
  private void onVerdictTimeout() {
    javafx.application.Platform.runLater(
        () -> {
          hideVerdict();
          Alert alert =
              new Alert(
                  Alert.AlertType.INFORMATION, "Time’s up: no verdict was provided in 60 seconds.");
          alert.setHeaderText("Game Over");
          alert.showAndWait();
          javafx.application.Platform.exit();
        });
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

  // ----------------- Game Reset -----------------

  /**
   * Resets the game to the initial state with timer and start button visible. Called when player
   * chooses to replay after completing a game.
   */
  public void resetToMainRoom() {
    // Reset flashback tracking
    flashbackViewed = new boolean[] {false, false, false};

    // Reset timer
    remaining = START_SECONDS;
    updateTimerLabel(remaining);
    if (timeline != null) {
      timeline.stop();
    }
    setupTimer();

    // Reset UI to initial state
    initializeInitialState();

    // Hide any overlays
    hideOverlay();
    if (overlayRoot != null) {
      overlayRoot.getChildren().clear();
    }

    // Reset chat controller through game state context
    GameStateContext gameState = App.getGameStateContext();
    if (gameState != null && gameState.getChatController() != null) {
      // The chat controller will be reset when the new GameStateContext is created
      // but we ensure the UI is properly reset here
      try {
        // Reload chat.fxml to reset chat UI components
        loadAndInjectChatController();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  // ----------------- Cleanup -----------------

  public void shutdown() {
    if (timeline != null) timeline.stop();
  }
}
