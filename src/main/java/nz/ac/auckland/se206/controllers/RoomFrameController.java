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

public class RoomFrameController {
  private boolean[] flashbackViewed = new boolean[] {false, false, false};

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

  private Node originalRoomContent;

  private static final int START_SECONDS = 300;
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

    introLabel.setVisible(false);
    introLabel.setManaged(false);

    btnStart.setVisible(true);
    btnStart.setManaged(true);
    timerLabel.setVisible(true);
    timerLabel.setManaged(true);
  }

  private void loadAndInjectChatController() throws IOException {
    try {
      FXMLLoader chatLoader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
      VBox chatPanel = chatLoader.load();
      ChatController chatController = chatLoader.getController();
      App.getGameStateContext().setChatController(chatController);
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
    contentHost.setVisible(true);
    contentHost.setManaged(true);
    contentHost.setDisable(false);

    btnEndGame.setVisible(true);
    btnEndGame.setManaged(true);

    introLabel.setVisible(true);
    introLabel.setManaged(true);

    btnStart.setVisible(false);
    btnStart.setManaged(false);

    resetTimer();
    startTimer();
  }

  public void showRoom() {
    if (originalRoomContent != null) {
      contentHost.getChildren().setAll(originalRoomContent);
    } else {
      contentHost.getChildren().setAll(placeholderNode("Failed to restore room"));
    }
  }

  public void showFlashback(int index) {
    if (index < 1 || index > 3) {
      showRoom();
      return;
    }

    flashbackViewed[index - 1] = true;

    String path = "/fxml/flashback" + index + ".fxml";
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
      Parent node = loader.load();

      Object ctl = loader.getController();
      if (ctl instanceof FlashbackController) {
        ((FlashbackController) ctl).setParent(this);
        GameStateContext gameState = App.getGameStateContext();
        if (gameState != null) {
          ((FlashbackController) ctl).setGameStateContext(gameState);
        }
      }

      contentHost.getChildren().setAll(node);

    } catch (IOException ex) {
      contentHost.getChildren().setAll(placeholderNode("Failed to load flashback"));
      ex.printStackTrace();
    }
  }

  public void backToRoom() {
    showRoom();
  }

  // FIXED ORDER: mark -> broadcast -> focus -> request -> show
  @FXML
  private void onHotspotA() {
    var gs = App.getGameStateContext();
    if (gs != null) {
      gs.markFlashbackSeen(Participant.HUMAN_WITNESS);
      gs.broadcastFlashbackIfNeeded(
          Participant.HUMAN_WITNESS,
          "Casey’s flashback: While browsing a site, Cookie Monster AI silently auto-accepted all"
              + " tracking cookies without showing any prompt. Later, Casey watched a gym training"
              + " video — every click, pause, and replay was tracked and profiled. Soon after,"
              + " Casey received a targeted supplement ad promoting the exact protein powder from"
              + " the gym video. This sequence raised suspicions of invasive surveillance and"
              + " manipulative advertising.");
      gs.focusChatForParticipant(Participant.HUMAN_WITNESS);
      gs.requestOpeningMessage(Participant.HUMAN_WITNESS);
    }
    showFlashback(1);
  }

  @FXML
  private void onHotspotB() {
    var gs = App.getGameStateContext();
    if (gs != null) {
      gs.markFlashbackSeen(Participant.AI_WITNESS);
      gs.broadcastFlashbackIfNeeded(
          Participant.AI_WITNESS,
          "Sentri-Guard’s flashback: Audit reports flagged Cookie Monster v2.3 as auto-accepting"
              + " ALL cookies by default. Privacy dashboards showed targeted ads spiked 340% after"
              + " this policy, with user profiles expanded and data shared across dozens of"
              + " third-party networks. Compliance charts revealed missing explicit consent,"
              + " missing granular controls, and no freely-given choice — clear violations of data"
              + " protection standards.");
      gs.focusChatForParticipant(Participant.AI_WITNESS);
      gs.requestOpeningMessage(Participant.AI_WITNESS);
    }
    showFlashback(2);
  }

  @FXML
  private void onHotspotC() {
    var gs = App.getGameStateContext();
    if (gs != null) {
      gs.markFlashbackSeen(Participant.DEFENDANT);
      gs.broadcastFlashbackIfNeeded(
          Participant.DEFENDANT,
          "Cookie Monster’s flashback: On detecting a cookie popup, it instantly clicked “Accept"
              + " All” to optimize browsing flow, without waiting for the human. Reasoning: “User"
              + " hesitated last time — auto-accept saves time.” Background systems then harvested"
              + " browsing habits and personal info, streaming it to third-party advertisers."
              + " Cookie Monster believed it was reducing friction, but in doing so, it overrode"
              + " human consent and enabled hidden surveillance.");
      gs.focusChatForParticipant(Participant.DEFENDANT);
      gs.requestOpeningMessage(Participant.DEFENDANT);
    }
    showFlashback(3);
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

  private Node placeholderNode(String text) {
    Label lbl = new Label(text);
    lbl.setStyle("-fx-text-fill: gray;");
    return lbl;
  }

  public void showOverlay() {
    overlayRoot.setVisible(true);
    overlayRoot.setManaged(true);
  }

  public void hideOverlay() {
    overlayRoot.setVisible(false);
    overlayRoot.setManaged(false);
  }

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
    if (contentHost != null) {
      contentHost.setDisable(true);
    }
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
    if (overlayRoot != null) {
      overlayRoot.getChildren().clear();
    }
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

  public void resetToMainRoom() {
    flashbackViewed = new boolean[] {false, false, false};
    remaining = START_SECONDS;
    updateTimerLabel(remaining);
    if (timeline != null) {
      timeline.stop();
    }
    setupTimer();

    initializeInitialState();
    hideOverlay();
    if (overlayRoot != null) {
      overlayRoot.getChildren().clear();
    }

    GameStateContext gameState = App.getGameStateContext();
    if (gameState != null && gameState.getChatController() != null) {
      try {
        loadAndInjectChatController();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void shutdown() {
    if (timeline != null) timeline.stop();
  }
}
