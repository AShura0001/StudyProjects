package nz.ac.auckland.se206.controllers;

import java.io.IOException;
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
import nz.ac.auckland.se206.persona.AwarenessFormatter;
import nz.ac.auckland.se206.persona.ConversationBus;
import nz.ac.auckland.se206.speech.TextToSpeech;

/** Main room frame controller: hosts the room, overlays, and navigation to slideshows/memories. */
public class RoomFrameController implements GameStateContext.RoomUiFacade {

  @FXML private StackPane contentHost;
  @FXML private StackPane overlayRoot;
  @FXML private Button btnStart;
  @FXML private Button btnEndGame;
  @FXML private Label timerLabel;
  @FXML private Label introLabel;
  @FXML private VBox chatBox;
  @FXML private ImageView roomImage;
  @FXML private StackPane imageHost;
  @FXML private Button hotspotA;
  @FXML private Button hotspotB;
  @FXML private Button hotspotC;

  // Store original room content for restoration
  private Node originalRoomContent;

  // Timer (5 minutes)
  private static final int START_SECONDS = 300;
  private int remaining = START_SECONDS;
  private Timeline timeline;

  @FXML
  public void initialize() {
    // Register this room UI with the shared GameStateContext
    GameStateContext gameState = App.getGameStateContext();
    if (gameState != null) {
      gameState.setRoomUi(this);
    }

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

    if (btnEndGame != null) {
      btnEndGame.setVisible(false);
      btnEndGame.setManaged(false);
    }

    if (introLabel != null) {
      introLabel.setVisible(false);
      introLabel.setManaged(false);
    }

    if (btnStart != null) {
      btnStart.setVisible(true);
      btnStart.setManaged(true);
    }
    if (timerLabel != null) {
      timerLabel.setVisible(true);
      timerLabel.setManaged(true);
    }
  }

  /** Loads chat.fxml and injects the ChatController into GameStateContext. */
  private void loadAndInjectChatController() throws IOException {
    try {
      FXMLLoader chatLoader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
      VBox chatPanel = chatLoader.load();
      ChatController chatController = chatLoader.getController();

      GameStateContext gameState = App.getGameStateContext();
      if (gameState != null) {
        gameState.setChatController(chatController);
      }

      // Replace the placeholder chatBox content with the real chat panel content
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
    // Fresh round: clear any prior awareness so nobody "knows" anything up front.
    GameStateContext gs = App.getGameStateContext();
    if (gs != null) {
      gs.resetAwareness();
    }

    // Show and enable all game content
    contentHost.setVisible(true);
    contentHost.setManaged(true);
    contentHost.setDisable(false);

    if (btnEndGame != null) {
      btnEndGame.setVisible(true);
      btnEndGame.setManaged(true);
    }

    if (introLabel != null) {
      introLabel.setVisible(true);
      introLabel.setManaged(true);
    }

    if (btnStart != null) {
      btnStart.setVisible(false);
      btnStart.setManaged(false);
    }

    TextToSpeech.speak(
        "In the Court of Common Clicks, Cookie Monster v2.3 is on trial for auto-accepting cookies "
            + "to reduce pop-ups. Casey’s targeted ads and Sentri-Guard’s audit reveal a "
            + "friction-first design that ignored consent, turning convenience into a question of "
            + "accountability.");

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

  // Allow flashbacks to navigate back
  public void backToRoom() {
    showRoom();
    showMainCourtUIForRoom();
  }

  /** Hides main courtroom UI elements that shouldn't appear in memory scenes */
  private void hideMainCourtUIForMemory() {
    if (introLabel != null) {
      introLabel.setVisible(false);
      introLabel.setManaged(false);
    }
    if (btnEndGame != null) {
      btnEndGame.setVisible(false);
      btnEndGame.setManaged(false);
    }
    if (timerLabel != null) {
      timerLabel.setVisible(true);
      timerLabel.setManaged(true);
    }
  }

  /** Shows main courtroom UI elements when returning to the main room */
  private void showMainCourtUIForRoom() {
    if (introLabel != null) {
      introLabel.setVisible(true);
      introLabel.setManaged(true);
    }
    if (btnEndGame != null) {
      btnEndGame.setVisible(true);
      btnEndGame.setManaged(true);
    }
    if (timerLabel != null) {
      timerLabel.setVisible(true);
      timerLabel.setManaged(true);
    }
  }

  // Show slideshow (flashback) for first-time interaction
  public void showSlideshow(Participant participant) {
    String path = "/fxml/slideshow.fxml";
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
      Parent node = loader.load();

      Object controller = loader.getController();
      if (controller instanceof SlideshowController) {
        SlideshowController slideshowController = (SlideshowController) controller;
        slideshowController.setParent(this);
        slideshowController.setGameStateContext(App.getGameStateContext());
        slideshowController.setCurrentParticipant(participant);
      }

      if (originalRoomContent == null && !contentHost.getChildren().isEmpty()) {
        originalRoomContent = contentHost.getChildren().get(0);
      }

      contentHost.getChildren().setAll(node);
      hideMainCourtUIForMemory();

      // Broadcast this flashback to everyone's awareness immediately
      broadcastFlashback(participant);

    } catch (Exception ex) {
      contentHost.getChildren().setAll(placeholderNode("Failed to load slideshow"));
      ex.printStackTrace();
    }
  }

  // Show memory scene for participant
  public void showMemoryForParticipant(Participant participant) {
    String fxmlPath = getMemoryFxmlPath(participant);
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Parent node = loader.load();

      Object controller = loader.getController();
      if (controller instanceof MemoryController) {
        MemoryController memoryController = (MemoryController) controller;
        memoryController.setParent(this);
        memoryController.setGameStateContext(App.getGameStateContext());
        memoryController.setCurrentParticipant(participant);
      }

      if (originalRoomContent == null && !contentHost.getChildren().isEmpty()) {
        originalRoomContent = contentHost.getChildren().get(0);
      }

      contentHost.getChildren().setAll(node);
      hideMainCourtUIForMemory();

      GameStateContext gameState = App.getGameStateContext();
      if (gameState != null) {
        gameState.focusChatForParticipant(participant);
      }

    } catch (Exception ex) {
      contentHost.getChildren().setAll(placeholderNode("Failed to load memory scene"));
      ex.printStackTrace();
    }
  }

  private String getMemoryFxmlPath(Participant participant) {
    switch (participant) {
      case DEFENDANT:
        return "/fxml/memory_defendant.fxml";
      case AI_WITNESS:
        return "/fxml/memory_ai_witness.fxml";
      case HUMAN_WITNESS:
        return "/fxml/memory_human_witness.fxml";
      default:
        return "/fxml/memory_defendant.fxml";
    }
  }

  @FXML
  private void onHotspotA() { // Human Witness
    GameStateContext gameState = App.getGameStateContext();
    Participant participant = ChatController.Participant.HUMAN_WITNESS;
    if (gameState != null) {
      if (gameState.isFirstInteraction(participant)) showSlideshow(participant);
      else showMemoryForParticipant(participant);
    }
  }

  @FXML
  private void onHotspotB() { // AI Witness
    GameStateContext gameState = App.getGameStateContext();
    Participant participant = ChatController.Participant.AI_WITNESS;
    if (gameState != null) {
      if (gameState.isFirstInteraction(participant)) showSlideshow(participant);
      else showMemoryForParticipant(participant);
    }
  }

  @FXML
  private void onHotspotC() { // Defendant
    GameStateContext gameState = App.getGameStateContext();
    Participant participant = ChatController.Participant.DEFENDANT;
    if (gameState != null) {
      if (!gameState.hasSeenSlideshow(participant)) {
        gameState.markSlideshowSeen(participant); // mark immediately
        showSlideshow(participant);
      } else {
        showMemoryForParticipant(participant);
      }
    }
  }


  private String getRoleKeyForParticipant(Participant participant) {
    if (participant == null) return "ai_defendant";
    switch (participant) {
      case HUMAN_WITNESS:
        return "human_witness";
      case AI_WITNESS:
        return "ai_witness";
      case DEFENDANT:
      default:
        return "ai_defendant";
    }
  }

  private String flashbackSummary(Participant p) {
    switch (p) {
      case HUMAN_WITNESS:
        return "Casey’s(Human witness) flashback: While browsing a site, Cookie Monster"
            + " auto-accepted all tracking cookies without asking; later a gym video watch"
            + " was fully tracked, followed by a targeted supplement ad matching that"
            + " video.";
      case AI_WITNESS:
        return "Sentri-Guard’s (ai witness) flashback: Audit flagged Cookie Monster v2.3"
            + " auto-accepting ALL cookies; targeted ads spiked 340% and data flowed to many"
            + " third parties; explicit/granular consent was missing.";
      case DEFENDANT:
      default:
        return "Cookie Monster’s (ai defendant) flashback: On cookie popup, it clicked “Accept All”"
            + " to reduce friction based on past hesitation; background collection then"
            + " shared browsing data with advertisers, overriding user consent.";
    }
  }

  @FXML
  private void onEndGame() {
    GameStateContext gameState = App.getGameStateContext();
    if (gameState != null && !gameState.hasContactedAllParticipants()) {
      showIncompleteContactAlert(gameState);
      return;
    }

    if (timeline != null) timeline.stop();
    lockRoomInteractions();
    showVerdictWithCountdown();
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
                    GameStateContext gameState = App.getGameStateContext();
                    if (gameState != null && !gameState.hasContactedAllParticipants()) {
                      showGameOverAlert(gameState);
                    } else {
                      lockRoomInteractions();
                      showVerdictWithCountdown();
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

  private void lockRoomInteractions() {
    if (contentHost != null) contentHost.setDisable(true);
  }

  private void showVerdictWithCountdown() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/verdict.fxml"));
      Node verdictNode = loader.load();

      VerdictController controller = loader.getController();
      controller.setFrame(this);
      controller.showQuestion("the AI defendant");

      overlayRoot.getChildren().setAll(verdictNode);
      showOverlay();

      try {
        Method m = controller.getClass().getMethod("startCountdown", int.class, Runnable.class);
        m.invoke(controller, 60, (Runnable) this::onVerdictTimeout);
      } catch (NoSuchMethodException nsme) {
        // older verdict controllers without countdown—ignore
      } catch (Exception reflectionError) {
        reflectionError.printStackTrace();
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

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

  public void hideVerdict() {
    hideOverlay();
    if (overlayRoot != null) overlayRoot.getChildren().clear();
  }

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

  /** Default flashback title per role (used in broadcast lines). */
  private String defaultFlashbackTitle(Participant p) {
    switch (p) {
      case HUMAN_WITNESS:
        return "Casey’s Memory";
      case AI_WITNESS:
        return "Sentri-Guard Audit Snapshot";
      case DEFENDANT:
      default:
        return "Cookie Monster’s Memory";
    }
  }

  /** 🔧 Restored helper lost in merge: concise summary that personas can quote. */
  private String flashbackSummary(Participant p) {
    switch (p) {
      case HUMAN_WITNESS:
        return "Casey searched gyms/supplements privately; targeted ads appeared; frustration high"
                   + " due to non-consensual tracking.";
      case AI_WITNESS:
        return "Audit shows allowed vs blocked events; consent bypass on cross-site tracking →"
                   + " targeted supplement ads.";
      case DEFENDANT:
      default:
        return "Cookie Monster optimized for fast cookie acceptance; consent and friction"
                   + " considerations deferred.";
    }
  }

  /** 🔧 Restored helper lost in merge: role key for AwarenessFormatter. */
  private String getRoleKeyForParticipant(Participant p) {
    switch (p) {
      case HUMAN_WITNESS:
        return "human_witness";
      case AI_WITNESS:
        return "ai_witness";
      case DEFENDANT:
      default:
        return "ai_defendant";
    }
  }

  /** Broadcast a flashback view to Awareness + Bus so all personas "hear" it. */
  private void broadcastFlashback(Participant p) {
    String title = defaultFlashbackTitle(p);
    String summary = flashbackSummary(p);
    String roleKey = getRoleKeyForParticipant(p);

    try {
      AwarenessFormatter.addMemoryEvent(
          "flashback", "Flashback viewed → " + title + " — " + summary);
      AwarenessFormatter.setFlashbackContext(roleKey, summary);
    } catch (Throwable ignored) {
    }

    try {
      ConversationBus.getInstance()
          .post("room", "[AWARENESS] Flashback viewed → " + title + " — " + summary);
    } catch (Throwable ignored) {
    }
  }

  // ----------------- Game Reset -----------------

  /** Reset the game to the initial state. */
  public void resetToMainRoom() {
    remaining = START_SECONDS;
    updateTimerLabel(remaining);
    if (timeline != null) timeline.stop();
    setupTimer();

    initializeInitialState();

    hideOverlay();
    if (overlayRoot != null) overlayRoot.getChildren().clear();

    GameStateContext gameState = App.getGameStateContext();
    if (gameState != null && gameState.getChatController() != null) {
      try {
        loadAndInjectChatController();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  // -------- RoomUiFacade (GameStateContext → Room) --------
  @Override
  public void lockInteractions() {
    lockRoomInteractions();
  }

  @Override
  public void unlockInteractions() {
    if (contentHost != null) contentHost.setDisable(false);
  }
}
