package nz.ac.auckland.se206.controllers;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import nz.ac.auckland.se206.GameStateContext;
import nz.ac.auckland.se206.controllers.ChatController.Participant;
import nz.ac.auckland.se206.persona.AwarenessFormatter; // push rows/memory into AWARENESS
import nz.ac.auckland.se206.persona.ConversationBus; // awareness broadcast bus;

/**
 * Controller for memory scenes where players interact with participants in their memory spaces with
 * unique interactable elements. Also implements the AI Witness "Simple Audit Log".
 */
public class MemoryController {
  private RoomFrameController parent;
  private GameStateContext gameStateContext;
  private Participant currentParticipant;

  // Common UI elements for all memory scenes
  @FXML private StackPane memoryContainer;
  @FXML private ImageView memoryBackground;
  @FXML private Button btnBackToRoom;

  // Chat components
  @FXML private Label lblStatus;
  @FXML private TextArea txtaChat;
  @FXML private TextField txtInput;
  @FXML private Button btnSend;

  // Interactable elements (specific to each memory scene)
  @FXML private Button interactableElement;
  @FXML private Rectangle interactableHighlight;
  @FXML private Label interactableLabel;
  @FXML private VBox interactableContainer;

  // ===== Human witness: Frustration mini-game (supports BOTH UIs) =====
  @FXML private VBox frustrationSection;
  @FXML private Slider frustrationSlider;
  @FXML private Label frustrationValueLabel;

  // Style A: "Higher" / "Lower" buttons
  @FXML private Button btnHigher;
  @FXML private Button btnLower;
  @FXML private VBox frustrationDialogue;
  @FXML private Label frustrationSpeaker; // optional
  @FXML private Label frustrationDialogueText;

  // Style B: single "Guess Level" button + context reveal
  @FXML private Button btnUnderstandStory;
  @FXML private VBox contextRevelation;
  @FXML private Label contextDetails;

  // Hint labels for defendant and AI witness (similar to human witness context revelation)
  @FXML private VBox defendantHintBox;
  @FXML private Label defendantHintLabel;
  @FXML private VBox aiWitnessHintBox;
  @FXML private Label aiWitnessHintLabel;

  // ===== AI witness: extra controls =====
  @FXML private Button btnAnalyzeMemory;
  @FXML private Button btnValidateMemory;
  @FXML private VBox memoryLog;

  // Aim training game components (for defendant memory)
  @FXML private VBox aimTrainingGame;
  @FXML private Label lblScore;
  @FXML private Label lblMisses;
  @FXML private Label lblTimer;
  @FXML private Label lblGameStatus;
  @FXML private Button btnStartGame;
  @FXML private Pane gamePlayArea;
  @FXML private VBox gameResults;
  @FXML private Label lblGameResult;
  @FXML private Label lblFinalStats;

  // ---- Simple Audit Log ----
  @FXML private VBox auditPanel;
  @FXML private Button btnLoadSample;
  @FXML private Button btnFilterAllowed;
  @FXML private Button btnFilterBlocked;
  @FXML private Button btnResetFilters;
  @FXML private Button btnExplainSelected;
  @FXML private Button btnCloseAudit;

  @FXML private TableView<AuditRow> auditTable;
  @FXML private TableColumn<AuditRow, String> colTime;
  @FXML private TableColumn<AuditRow, String> colSite;
  @FXML private TableColumn<AuditRow, String> colAction;
  @FXML private TableColumn<AuditRow, String> colAllowed;
  @FXML private Label auditStatus;
  @FXML private Label auditTitle;

  private final ObservableList<AuditRow> auditMaster = FXCollections.observableArrayList();
  private FilteredList<AuditRow> auditFiltered;

  // ===== State tracking =====
  private boolean interactableUsed = false;
  private int targetFrustrationLevel = 8; // unified default
  private boolean frustrationGuessed = false;
  
  // Hint reveal tracking
  private boolean defendantHintRevealed = false;
  private boolean aiWitnessHintRevealed = false;

  // Aim training game state
  private int gameScore = 0;
  private int totalMisses = 0;
  private boolean gameRunning = false;
  private Timeline gameTimer;
  private Timeline targetSpawnTimer;
  private final List<Button> activeTargets = new ArrayList<>();
  private final Random random = new Random();
  private long gameStartTime = 0;

  // Theme tracking (Matrix theme for AI_WITNESS)
  private boolean matrixThemeApplied = false;

  public void setParent(RoomFrameController parent) {
    this.parent = parent;
  }

  public void setGameStateContext(GameStateContext gameStateContext) {
    this.gameStateContext = gameStateContext;
    // Wire up flashback chat components for cross-controller chat mirroring
    if (gameStateContext != null && gameStateContext.getChatController() != null) {
      gameStateContext
          .getChatController()
          .setFlashbackChatComponents(lblStatus, txtaChat, txtInput, btnSend);
    }
    enableChatAutoScroll();
  }

  public void setCurrentParticipant(Participant participant) {
    this.currentParticipant = participant;
    updateMemoryContext();
    updateFrustrationSectionVisibility();
    updateAiWitnessButtonState();
    updateAimTrainingGameVisibility();
    updateChatControlsState();
    applyThemeForParticipant();
  }

  // --- Cookie-consent handlers (AI Witness doesn't use them, but FXML references exist) ---
  @FXML
  private void onAcceptCookies() {
    // No-op in the AI Witness scene. (Present only to satisfy the FXML loader.)
  }

  @FXML
  private void onDeclineCookies() {
    // Safe fallback: just go back to the room if user clicks decline on a misplaced overlay.
    if (parent != null) {
      parent.backToRoom();
    }
  }

  @FXML
  private void initialize() {
    if (interactableHighlight != null) {
      interactableHighlight.setVisible(false);
      interactableHighlight.setMouseTransparent(true);
    }
    if (interactableElement != null) {
      interactableElement.setPickOnBounds(true);
      interactableElement.setOnMouseEntered(e -> showInteractableHighlight());
      interactableElement.setOnMouseExited(e -> hideInteractableHighlight());
    }
    if (frustrationSlider != null && frustrationValueLabel != null) {
      frustrationSlider
          .valueProperty()
          .addListener(
              (obs, oldVal, newVal) ->
                  frustrationValueLabel.setText(String.valueOf(newVal.intValue())));
    }
    enableChatAutoScroll();
    initAuditTable();
  }

  // === BROADCAST / AWARENESS HELPERS =========================================

  /** Post to bus+chat and also store a concise awareness event. */
  private void broadcastToAll(String awarenessTag, String summaryLine) {
    try {
      AwarenessFormatter.addMemoryEvent(awarenessTag, summaryLine);
    } catch (Throwable ignored) {
    }
    postToBusAndChat("[AWARENESS] " + summaryLine);
  }

  /** Formats a participant-scoped memory summary and broadcasts it. */
  private void broadcastMemorySummary(Participant p, String summaryCore) {
    String tag = (p == null) ? "room" : getRoleKeyOf(p);
    broadcastToAll(tag, "Memory summary (" + tag + "): " + summaryCore);
  }

  /** Public: other controllers can broadcast flashback summaries through here. */
  public void publishFlashbackSummary(String title, String summary) {
    String line = "Flashback viewed → " + title + " — " + summary;
    broadcastToAll("flashback", line);
  }

  // ---------------- Navigation ----------------
  @FXML
  private void onBackToRoom() {
    if (parent != null) parent.backToRoom();
  }

  // ---------------- Chat send ----------------
  @FXML
  private void onSendMessage() {
    // Gate Cookie Monster chat until the aim trainer is completed
    if (currentParticipant == Participant.DEFENDANT && !interactableUsed) {
      if (txtaChat != null) {
        String restrictionMessage =
            "\n🔒 System: Complete Cookie Monster's aim training before chatting.\n";
        String currentText = txtaChat.getText();
        if (!currentText.contains("Complete Cookie Monster's aim training")) {
          txtaChat.setText(currentText + restrictionMessage);
        }
      }

      // Clear the input field
      if (txtInput != null) {
        txtInput.clear();
      }
      return;
    }

    // Allow chat for all participants (after completing interactive element)

    if (gameStateContext != null && gameStateContext.getChatController() != null) {
      gameStateContext.getChatController().onSendMessage();
    }
  }

  // ---------------- Interactable click ----------------
  @FXML
  private void onInteractableClick() {
    if (gameStateContext == null || currentParticipant == null) return;

    String roleKey = getRoleKeyOf(currentParticipant);
    gameStateContext.markInteractableDone(roleKey);
    interactableUsed = true;
    updateInteractableState();

    if (currentParticipant == Participant.AI_WITNESS) {
      // Show Simple Audit Log panel
      if (interactableContainer != null) interactableContainer.setVisible(false);
      if (auditPanel != null) {
        auditPanel.setVisible(true);
        auditPanel.setManaged(true);
        auditPanel.toFront();
      }
      setAuditStatus("Tap Load");

      // Awareness + broadcast
      try {
        AwarenessFormatter.addMemoryEvent("ai_witness", "Opened Simple Audit Log");
      } catch (Throwable ignored) {
      }
      broadcastAuditOpenedAwareness();
    }

    notifyChatAboutInteraction();

    // Broadcast that this memory was opened
    broadcastMemorySummary(
        currentParticipant,
        currentParticipant == Participant.AI_WITNESS
            ? "Opened AI Witness ‘Simple Audit Log’ (allowed vs blocked; consent focus)."
            : currentParticipant == Participant.HUMAN_WITNESS
                ? "Opened Human Witness evidence; emotion/impact assessment available."
                : "Opened AI Defendant journal (cookie-gated chat).");

    updateAiWitnessButtonState();
  }

  /** Broadcast a room-wide awareness line so all personas know this memory was opened. */
  private void broadcastAuditOpenedAwareness() {
    String msg =
        "Memory used → AI Witness (Sentri-Guard): Simple Audit Log opened; shows ALLOWED (no"
            + " consent) vs BLOCKED (human) events, including a path ‘watched gym video →"
            + " cross-site tracking → targeted supplement ads’.";
    postToBusAndChat("[AWARENESS] " + msg);
  }

  /** Broadcast each audit row verbatim so personas can quote it exactly. */
  private void broadcastAuditRows() {
    postToBusAndChat("[AWARENESS] Simple Audit Log (Sentri-Guard):");
    for (AuditRow r : auditMaster) {
      String line =
          "\""
              + r.getSite()
              + " — "
              + r.getAction()
              + " — Allowed? "
              + r.getAllowed()
              + "\" (Source: Sentri-Guard / AI Witness)";
      postToBusAndChat(line);
    }
  }

  /** Push all audit rows into AWARENESS so the LLMs can recall them exactly. */
  private void syncAwarenessWithAuditRows() {
    AwarenessFormatter.clearAuditItems();
    for (AuditRow r : auditMaster) {
      AwarenessFormatter.addAuditItem(r.getSite(), r.getAction(), r.getAllowed());
    }
  }

  /** Helper to broadcast to the ConversationBus and mirror to local chat. */
  private void postToBusAndChat(String message) {
    try {
      ConversationBus.getInstance().post("room", message);
    } catch (Throwable ignored) {
    }
    if (txtaChat != null) txtaChat.appendText(message + "\n");
  }

  private void updateMemoryContext() {
    if (gameStateContext == null || currentParticipant == null) return;

    String roleKey = getRoleKeyOf(currentParticipant);
    interactableUsed = gameStateContext.getInteractableDone().getOrDefault(roleKey, false);

    if (currentParticipant == Participant.HUMAN_WITNESS) {
      frustrationGuessed = interactableUsed;
      updateFrustrationButtonState();
    }

    if (currentParticipant == Participant.AI_WITNESS && interactableUsed) {
      if (interactableContainer != null) interactableContainer.setVisible(false);
      if (auditPanel != null) {
        auditPanel.setVisible(true);
        auditPanel.setManaged(true);
        auditPanel.toFront();
      }
    }

    updateInteractableState();
  }

  private String getRoleKeyOf(Participant participant) {
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

  private void updateInteractableState() {
    if (interactableElement != null && interactableLabel != null) {
      if (interactableUsed) {
        interactableElement.setDisable(true);
        interactableElement.setStyle("-fx-opacity: 0.6;");
        interactableLabel.setText("Already opened");
      } else {
        interactableElement.setDisable(false);
        interactableElement.setStyle("-fx-opacity: 1.0;");
        interactableLabel.setText("Open the Simple Audit Log");
      }
    }
  }

  private void showInteractableHighlight() {
    if (interactableHighlight != null && !interactableUsed) {
      interactableHighlight.setVisible(true);
    }
  }

  private void hideInteractableHighlight() {
    if (interactableHighlight != null) {
      interactableHighlight.setVisible(false);
    }
  }

  private void notifyChatAboutInteraction() {
    if (gameStateContext != null
        && gameStateContext.getChatController() != null
        && currentParticipant != null) {
      String interactionMessage = getInteractionMessage();
      if (txtaChat != null) {
        txtaChat.appendText("[SYSTEM] " + interactionMessage + "\n\n");
      }
    }
  }

  private String getInteractionMessage() {
    switch (currentParticipant) {
      case DEFENDANT:
        return "You checked the defendant’s journal.";
      case AI_WITNESS:
        return "You opened the audit log (allowed vs blocked).";
      case HUMAN_WITNESS:
        return "You reviewed the witness’s evidence.";
      default:
        return "You examine the memory object.";
    }
  }

  public boolean isInteractableUsed() {
    return interactableUsed;
  }

  private void requestOpeningMessage() {
    if (gameStateContext != null
        && gameStateContext.getChatController() != null
        && currentParticipant != null) {
      // Don't mark as contacted here - only mark when leaving after actual chat
      gameStateContext.getChatController().selectParticipant(currentParticipant);
      gameStateContext.getChatController().requestOpeningMessageFromLlm(currentParticipant);
    }
  }

  // ---------------- Human frustration mini-game ----------------
  @FXML
  private void onFrustrationHigher() {
    if (frustrationGuessed) return;
    int currentGuess =
        (int) (frustrationSlider != null ? frustrationSlider.getValue() : targetFrustrationLevel);
    evaluateFrustrationGuess(currentGuess, true);
  }

  @FXML
  private void onFrustrationLower() {
    if (frustrationGuessed) return;
    int currentGuess =
        (int) (frustrationSlider != null ? frustrationSlider.getValue() : targetFrustrationLevel);
    evaluateFrustrationGuess(currentGuess, true);
  }

  @FXML
  private void onUnderstandStory() {
    if (frustrationGuessed) return;
    int currentGuess =
        (int) (frustrationSlider != null ? frustrationSlider.getValue() : targetFrustrationLevel);
    evaluateFrustrationGuess(currentGuess, false);
  }

  /** Shared logic for both UIs. */
  private void evaluateFrustrationGuess(int guess, boolean fromButtons) {
    if (guess == targetFrustrationLevel) {
      onCorrectFrustrationGuess();
    } else {
      String hint =
          (guess < targetFrustrationLevel)
              ? "I'm MORE frustrated than "
                  + guess
                  + " out of 10. This privacy violation really hurt me. Try higher."
              : "I'm upset but not quite " + guess + " out of 10. Try slightly lower.";
      showFrustrationDialogue(hint);
    }
  }

  private void onCorrectFrustrationGuess() {
    frustrationGuessed = true;

    if (btnHigher != null) {
      btnHigher.setDisable(true);
      btnHigher.setStyle("-fx-opacity: 0.5; -fx-background-color: #cccccc;");
    }
    if (btnLower != null) {
      btnLower.setDisable(true);
      btnLower.setStyle("-fx-opacity: 0.5; -fx-background-color: #cccccc;");
    }
    if (btnUnderstandStory != null) {
      btnUnderstandStory.setDisable(true);
      btnUnderstandStory.setText("Correct!");
      btnUnderstandStory.setStyle("-fx-opacity: 0.6; -fx-background-color: #4caf50;");
    }

    String successResponse =
        "EXACTLY! I'm at an "
            + targetFrustrationLevel
            + " out of 10! I searched for gym training programs and supplements, then got targeted"
            + " ads everywhere. I never consented to this tracking!";
    showFrustrationDialogue(successResponse);

    String contextualEvidence =
        "💡 Verdict Hint: Cookie Monster auto-accepted tracking cookies without Casey's consent. "
            + "Consider whether convenience should override informed consent.";
    showContextualEvidence(contextualEvidence);

    if (gameStateContext != null && currentParticipant == Participant.HUMAN_WITNESS) {
      gameStateContext.markInteractableDone(getRoleKeyOf(currentParticipant));
      interactableUsed = true;
    }

    // Broadcast outcome summary
    broadcastMemorySummary(
        Participant.HUMAN_WITNESS,
        "Witness frustration = "
            + targetFrustrationLevel
            + "/10; private gym/supplement searches led to ubiquitous ads; tracking occurred"
            + " without consent.");
  }

  private void showFrustrationDialogue(String dialogueText) {
    if (frustrationDialogue != null && frustrationDialogueText != null) {
      frustrationDialogueText.setText(dialogueText);
      frustrationDialogue.setVisible(true);
    }
  }

  private void showContextualEvidence(String evidenceText) {
    if (contextRevelation != null && contextDetails != null) {
      contextRevelation.setVisible(true);
      contextDetails.setText(""); // Clear existing text

      // Typewriter effect: reveal text character by character
      Timeline typewriterTimeline = new Timeline();
      for (int i = 0; i < evidenceText.length(); i++) {
        final int index = i;
        KeyFrame keyFrame =
            new KeyFrame(
                Duration.millis(30 * i), // 30ms delay per character
                event -> contextDetails.setText(evidenceText.substring(0, index + 1)));
        typewriterTimeline.getKeyFrames().add(keyFrame);
      }
      typewriterTimeline.play();
    }
  }

  /** Reveals hint for Cookie Monster (defendant) after completing aim training game */
  private void revealDefendantHint() {
    if (!defendantHintRevealed && defendantHintBox != null && defendantHintLabel != null) {
      defendantHintRevealed = true;
      defendantHintBox.setVisible(true);
      
      String hintText =
          "💡 Verdict Hint: Cookie Monster's training optimized for speed and efficiency metrics, "
              + "not user consent or privacy. The rapid auto-acceptance you just experienced shows how "
              + "the AI was programmed to bypass informed consent for convenience. Consider: Can an AI "
              + "act ethically when its success criteria ignore the core principle of user autonomy?";
      
      defendantHintLabel.setText(""); // Clear existing text
      
      // Typewriter effect for hint
      Timeline typewriterTimeline = new Timeline();
      for (int i = 0; i < hintText.length(); i++) {
        final int index = i;
        KeyFrame keyFrame =
            new KeyFrame(
                Duration.millis(30 * i),
                event -> defendantHintLabel.setText(hintText.substring(0, index + 1)));
        typewriterTimeline.getKeyFrames().add(keyFrame);
      }
      typewriterTimeline.play();
      
      // Add to awareness system
      broadcastToAll(
          "defendant",
          "Discovered: Cookie Monster's training prioritized efficiency KPIs over ethical consent practices");
    }
  }

  /** Reveals hint for Sentry Guard (AI witness) after interacting with audit log */
  private void revealAiWitnessHint() {
    if (!aiWitnessHintRevealed && aiWitnessHintBox != null && aiWitnessHintLabel != null) {
      aiWitnessHintRevealed = true;
      aiWitnessHintBox.setVisible(true);
      
      String hintText =
          "💡 Verdict Hint: The audit logs expose a clear pattern: Cookie Monster auto-accepted "
              + "tracking cookies that enabled cross-site surveillance, leading to targeted ads based on "
              + "Casey's private gym searches—all without explicit consent. The Sentry Guard's monitoring "
              + "system failed to flag these violations. Consider: When oversight fails and privacy is "
              + "violated for 'convenience,' can the system claim ethical operation?";
      
      aiWitnessHintLabel.setText(""); // Clear existing text
      
      // Typewriter effect for hint
      Timeline typewriterTimeline = new Timeline();
      for (int i = 0; i < hintText.length(); i++) {
        final int index = i;
        KeyFrame keyFrame =
            new KeyFrame(
                Duration.millis(30 * i),
                event -> aiWitnessHintLabel.setText(hintText.substring(0, index + 1)));
        typewriterTimeline.getKeyFrames().add(keyFrame);
      }
      typewriterTimeline.play();
      
      // Add to awareness system
      broadcastToAll(
          "ai_witness",
          "Discovered: Audit logs show systematic consent override patterns that weren't flagged by monitoring systems");
    }
  }

  private void updateFrustrationSectionVisibility() {
    if (frustrationSection != null) {
      boolean showFrustration = (currentParticipant == Participant.HUMAN_WITNESS);
      frustrationSection.setVisible(showFrustration);
      if (showFrustration) updateFrustrationButtonState();
    }
  }

  private void updateFrustrationButtonState() {
    if (frustrationGuessed) {
      if (btnHigher != null) {
        btnHigher.setDisable(true);
        btnHigher.setStyle("-fx-opacity: 0.5; -fx-background-color: #cccccc;");
      }
      if (btnLower != null) {
        btnLower.setDisable(true);
        btnLower.setStyle("-fx-opacity: 0.5; -fx-background-color: #cccccc;");
      }
      if (btnUnderstandStory != null) {
        btnUnderstandStory.setDisable(true);
        btnUnderstandStory.setText("Correct!");
        btnUnderstandStory.setStyle("-fx-opacity: 0.6; -fx-background-color: #4caf50;");
      }
      if (frustrationSlider != null) frustrationSlider.setValue(targetFrustrationLevel);
      showFrustrationDialogue(
          "EXACTLY! I'm at an "
              + targetFrustrationLevel
              + " out of 10! I searched for gym training programs and supplements, then got"
              + " targeted ads everywhere. I never consented to this tracking!");
      showContextualEvidence(
          "💡 Verdict Hint: Cookie Monster auto-accepted tracking cookies without Casey's consent. "
              + "Consider whether convenience should override informed consent.");
    } else {
      if (frustrationDialogue != null) frustrationDialogue.setVisible(false);
      if (contextRevelation != null) contextRevelation.setVisible(false);
      if (btnUnderstandStory != null) {
        btnUnderstandStory.setDisable(false);
        btnUnderstandStory.setText("🎯 Guess Level");
        btnUnderstandStory.setStyle(
            "-fx-font-size: 16px; -fx-background-color: #ff9800; -fx-text-fill: white;"
                + " -fx-pref-width: 150; -fx-pref-height: 45; -fx-border-radius: 8;"
                + " -fx-background-radius: 8;");
      }
      if (btnHigher != null) {
        btnHigher.setDisable(false);
        btnHigher.setStyle(
            "-fx-font-size: 16px; -fx-background-color: #ff5252; -fx-text-fill: white;"
                + " -fx-pref-width: 100; -fx-pref-height: 40; -fx-border-radius: 5;");
      }
      if (btnLower != null) {
        btnLower.setDisable(false);
        btnLower.setStyle(
            "-fx-font-size: 16px; -fx-background-color: #4caf50; -fx-text-fill: white;"
                + " -fx-pref-width: 100; -fx-pref-height: 40; -fx-border-radius: 5;");
      }
    }
  }

  // ---------------- AI witness extra actions (Analyze/Validate) ----------------
  private void updateAiWitnessButtonState() {
    if (currentParticipant != Participant.AI_WITNESS) return;

    if (interactableUsed) {
      if (btnAnalyzeMemory != null) {
        btnAnalyzeMemory.setDisable(true);
        btnAnalyzeMemory.setText("Data Analyzed");
        btnAnalyzeMemory.setStyle("-fx-opacity: 0.6; -fx-background-color: #cccccc;");
      }
      if (btnValidateMemory != null) {
        btnValidateMemory.setDisable(true);
        btnValidateMemory.setText("Validated");
        btnValidateMemory.setStyle("-fx-opacity: 0.6; -fx-background-color: #cccccc;");
      }
    } else {
      if (btnAnalyzeMemory != null) {
        btnAnalyzeMemory.setDisable(false);
        btnAnalyzeMemory.setText("Analyze Data");
        btnAnalyzeMemory.setStyle(
            "-fx-font-size: 14px; -fx-background-color: #ffebcd; -fx-text-fill: #222;"
                + " -fx-pref-width: 120;");
      }
      if (btnValidateMemory != null) {
        btnValidateMemory.setDisable(false);
        btnValidateMemory.setText("Validate");
        btnValidateMemory.setStyle(
            "-fx-font-size: 14px; -fx-background-color: #f0e68c; -fx-text-fill: #222;"
                + " -fx-pref-width: 120;");
      }
    }
  }

  @FXML
  private void onAnalyzeMemory() {
    if (gameStateContext != null && currentParticipant == Participant.AI_WITNESS) {
      String roleKey = getRoleKeyOf(currentParticipant);
      gameStateContext.markInteractableDone(roleKey);
      interactableUsed = true;
      if (btnAnalyzeMemory != null) {
        btnAnalyzeMemory.setDisable(true);
        btnAnalyzeMemory.setText("Data Analyzed");
        btnAnalyzeMemory.setStyle("-fx-opacity: 0.6; -fx-background-color: #cccccc;");
      }
      if (btnValidateMemory != null) {
        btnValidateMemory.setDisable(true);
        btnValidateMemory.setText("Validated");
        btnValidateMemory.setStyle("-fx-opacity: 0.6; -fx-background-color: #cccccc;");
      }
      notifyAiWitnessInteraction("Analyze Data");
      updateAiWitnessButtonState();
    }
  }

  @FXML
  private void onValidateMemory() {
    if (gameStateContext != null && currentParticipant == Participant.AI_WITNESS) {
      String roleKey = getRoleKeyOf(currentParticipant);
      gameStateContext.markInteractableDone(roleKey);
      interactableUsed = true;
      if (btnAnalyzeMemory != null) {
        btnAnalyzeMemory.setDisable(true);
        btnAnalyzeMemory.setText("Data Analyzed");
        btnAnalyzeMemory.setStyle("-fx-opacity: 0.6; -fx-background-color: #cccccc;");
      }
      if (btnValidateMemory != null) {
        btnValidateMemory.setDisable(true);
        btnValidateMemory.setText("Validated");
        btnValidateMemory.setStyle("-fx-opacity: 0.6; -fx-background-color: #cccccc;");
      }
      notifyAiWitnessInteraction("Validate");
      updateAiWitnessButtonState();
    }
  }

  /** Notifies the chat system about AI witness memory interaction */
  private void notifyAiWitnessInteraction(String action) {
    if (gameStateContext != null
        && gameStateContext.getChatController() != null
        && txtaChat != null) {
      String interactionMessage =
          "[SYSTEM] You "
              + action.toLowerCase()
              + "d the AI witness's memory data analysis core, accessing detailed incident logs and"
              + " processing records...";
      txtaChat.appendText(interactionMessage + "\n\n");

      if (gameStateContext.isFirstInteraction(currentParticipant)) {
        requestOpeningMessage();
      }
    }
  }

  /** Updates aim training game visibility based on current participant */
  private void updateAimTrainingGameVisibility() {
    if (aimTrainingGame != null) {
      boolean showGame = (currentParticipant == Participant.DEFENDANT && !interactableUsed);
      aimTrainingGame.setVisible(showGame);

      // Request opening message for Cookie Monster whether game is shown or completed
      if (currentParticipant == Participant.DEFENDANT && gameStateContext != null) {
        requestOpeningMessage();
      }
    }
  }

  /** Updates chat controls based on interactive element completion */
  private void updateChatControlsState() {
    if (currentParticipant == Participant.DEFENDANT) {
      boolean chatEnabled = interactableUsed;

      // Disable/enable chat input and send button
      if (txtInput != null) {
        txtInput.setDisable(!chatEnabled);
        if (!chatEnabled) {
          txtInput.setPromptText("Complete aim training to unlock chat...");
        } else {
          txtInput.setPromptText("Type a message...");
        }
      }

      if (btnSend != null) {
        btnSend.setDisable(!chatEnabled);
      }

      // Add a system message if chat is disabled

      if (!chatEnabled && txtaChat != null) {
        String currentText = txtaChat.getText();
        String lockMessage =
            "\n🔒 System: Complete Cookie Monster's aim training to unlock chat functionality.\n";
        if (!currentText.contains("Complete Cookie Monster's aim training to unlock")) {
          txtaChat.setText(currentText + lockMessage);
        }
      }
    } else {
      if (txtInput != null) {
        txtInput.setDisable(false);
        txtInput.setPromptText("Type a message...");
      }
      if (btnSend != null) btnSend.setDisable(false);
    }
  }

  // ---------------- AIM TRAINER ----------------

  @FXML
  private void onStartAimGame() {
    if (gameRunning) return;

    // Reset game state
    gameScore = 0;
    totalMisses = 0;
    gameRunning = true;
    gameStartTime = System.currentTimeMillis();
    activeTargets.clear();

    // Update UI
    updateGameStats();
    if (lblGameStatus != null)
      lblGameStatus.setText("Training in progress... Click the Accept buttons!");
    if (btnStartGame != null) {
      btnStartGame.setText("Training Active");
      btnStartGame.setDisable(true);
    }
    if (gamePlayArea != null) gamePlayArea.setVisible(true);
    if (gameResults != null) gameResults.setVisible(false);

    // Start the timers
    gameTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateGameTimer()));
    gameTimer.setCycleCount(Timeline.INDEFINITE);
    gameTimer.play();

    startTargetSpawning();
  }

  private void startTargetSpawning() {

    System.out.println("DEBUG: Starting target spawning system");

    targetSpawnTimer =
        new Timeline(
            new KeyFrame(Duration.millis(1250 + random.nextInt(2000)), e -> spawnTarget()));
    targetSpawnTimer.setCycleCount(Timeline.INDEFINITE);
    targetSpawnTimer.play();

    Platform.runLater(this::spawnTarget);
  }

  private void spawnTarget() {
    if (!gameRunning || gamePlayArea == null) return;

    Button target = new Button("Accept");
    target.setStyle(
        "-fx-font-size: 12px; -fx-background-color: #ffe066; -fx-text-fill: #222; "
            + "-fx-pref-width: 80; -fx-pref-height: 25; -fx-font-weight: bold; "
            + "-fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");

    double areaWidth =
        gamePlayArea.getWidth() > 0 ? gamePlayArea.getWidth() : gamePlayArea.getPrefWidth();
    double areaHeight =
        gamePlayArea.getHeight() > 0 ? gamePlayArea.getHeight() : gamePlayArea.getPrefHeight();

    // Random position within the play area (with some margin)
    double maxX = Math.max(0, areaWidth - 90); // button width + margin
    double maxY = Math.max(0, areaHeight - 35); // button height + margin
    double x = random.nextDouble() * maxX + 5; // 5px margin from edge
    double y = random.nextDouble() * maxY + 5; // 5px margin from edge

    target.setLayoutX(x);
    target.setLayoutY(y);
    target.setOnAction(e -> onTargetHit(target));

    gamePlayArea.getChildren().add(target);
    activeTargets.add(target);

    // Debug output
    System.out.println(
        "DEBUG: Spawned target at (" + x + ", " + y + ") in area " + areaWidth + "x" + areaHeight);

    // Set target to disappear after 0.85 seconds (decreased for easier gameplay)

    Timeline disappearTimer =
        new Timeline(new KeyFrame(Duration.seconds(0.85), e -> onTargetMiss(target)));
    disappearTimer.play();
  }

  private void onTargetHit(Button target) {
    if (!gameRunning) return;
    gamePlayArea.getChildren().remove(target);
    activeTargets.remove(target);
    gameScore++;
    updateGameStats();
    if (gameScore >= 5) endGame(true);
  }

  private void onTargetMiss(Button target) {
    if (!gameRunning || !activeTargets.contains(target)) return;
    gamePlayArea.getChildren().remove(target);
    activeTargets.remove(target);
    totalMisses++;
    updateGameStats();
    if (totalMisses >= 3) endGame(false);
  }

  private void updateGameStats() {
    if (lblScore != null) lblScore.setText(gameScore + "/5");
    if (lblMisses != null) {
      lblMisses.setText(totalMisses + "/3");
      if (totalMisses == 0) {
        lblMisses.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #059669;");
      } else if (totalMisses == 1) {
        lblMisses.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
      } else {
        lblMisses.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
      }
    }
  }

  private void updateGameTimer() {
    if (lblTimer != null && gameStartTime > 0) {
      long elapsed = (System.currentTimeMillis() - gameStartTime) / 1000;
      lblTimer.setText(elapsed + "s");
    }
  }

  /** Ends the game with win/lose result */
  private void endGame(boolean won) {
    gameRunning = false;
    if (gameTimer != null) gameTimer.stop();
    if (targetSpawnTimer != null) targetSpawnTimer.stop();

    for (Button target : new ArrayList<>(activeTargets)) {
      gamePlayArea.getChildren().remove(target);
    }
    activeTargets.clear();

    if (gamePlayArea != null) gamePlayArea.setVisible(false);
    if (gameResults != null) gameResults.setVisible(true);
    if (btnStartGame != null) {
      btnStartGame.setDisable(false);
    }

    if (won) {

      lblGameResult.setText("🎉 Training Complete!");
      lblGameResult.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #059669;");
      lblFinalStats.setText("You successfully completed Cookie Monster's auto-accept training!");
      btnStartGame.setText("Training Completed");
      btnStartGame.setStyle(
          "-fx-font-size: 16px; -fx-background-color: #22c55e; -fx-text-fill: white;"
              + " -fx-pref-width: 150; -fx-pref-height: 40; -fx-border-radius: 8;"
              + " -fx-background-radius: 8;");

      // Mark interactive element as completed
      if (gameStateContext != null && currentParticipant == Participant.DEFENDANT) {
        gameStateContext.markInteractableDone(getRoleKeyOf(currentParticipant));
        // Accept cookies since user completed Cookie Monster's auto-accept training
        gameStateContext.acceptCookies();
        interactableUsed = true;
        updateAimTrainingGameVisibility();
        updateChatControlsState();
      }

      // Notify chat + broadcast to awareness/bus so ALL personas know this happened
      notifyAimTrainingCompletion();
      // NEW: broadcast to all
      broadcastMemorySummary(
          Participant.DEFENDANT,
          "Completed Cookie Monster AIM training (auto-accept speed over consent). Chat unlocked.");
      
      // Reveal defendant hint after completing the game
      revealDefendantHint();
    } else {

      lblGameResult.setText("❌ Training Failed");
      lblGameResult.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
      lblFinalStats.setText("3 consecutive misses! Cookie Monster needs more training.");
      btnStartGame.setText("Retry Training");
      btnStartGame.setStyle(
          "-fx-font-size: 16px; -fx-background-color: #3b82f6; -fx-text-fill: white;"
              + " -fx-pref-width: 150; -fx-pref-height: 40; -fx-border-radius: 8;"
              + " -fx-background-radius: 8;");
    }
  }

  /** Notifies the chat system about aim training completion */
  private void notifyAimTrainingCompletion() {
    if (gameStateContext != null
        && gameStateContext.getChatController() != null
        && currentParticipant == Participant.DEFENDANT) {
      String interactionMessage =
          "You experienced Cookie Monster's auto-accept training simulation. The rapid-fire cookie"
              + " acceptance training reveals how the AI was programmed to prioritize speed over"
              + " user consent...";

      if (txtaChat != null) {
        String currentText = txtaChat.getText();
        String systemMessage = "[SYSTEM] " + interactionMessage + "\n\n";
        txtaChat.setText(currentText + systemMessage);
      }
      // Also post to bus so other personas “hear” it immediately
      postToBusAndChat(
          "[AWARENESS] Cookie Monster training complete → chat unlocked; system optimized for speed"
              + " > consent.");
    }
  }

  // ---------------- Chat helpers ----------------
  private void enableChatAutoScroll() {
    if (txtaChat == null) return;
    if (Boolean.TRUE.equals(txtaChat.getProperties().get("autoscrollBound"))) return;
    txtaChat
        .textProperty()
        .addListener(
            (obs, oldText, newText) -> {
              txtaChat.positionCaret(newText == null ? 0 : newText.length());
              txtaChat.setScrollTop(Double.MAX_VALUE);
            });
    txtaChat.getProperties().put("autoscrollBound", Boolean.TRUE);
  }

  private void applyThemeForParticipant() {
    if (txtaChat == null || txtInput == null || btnSend == null) return;

    if (currentParticipant == Participant.AI_WITNESS) {
      if (matrixThemeApplied) return;

      txtaChat.setStyle(
          "-fx-control-inner-background: black;"
              + "-fx-text-fill: #00ff88;"
              + "-fx-highlight-fill: #00ff88;"
              + "-fx-highlight-text-fill: black;"
              + "-fx-font-family: 'Consolas','Menlo','Monaco',monospace;"
              + "-fx-font-size: 15px;");
      txtInput.setStyle(
          "-fx-background-color: black;"
              + "-fx-text-fill: #00ff88;"
              + "-fx-prompt-text-fill: #2ddf93;"
              + "-fx-border-color: #00ff88;"
              + "-fx-font-family: 'Consolas','Menlo','Monaco',monospace;"
              + "-fx-font-size: 15px;");
      btnSend.setStyle(
          "-fx-font-size: 15px;"
              + "-fx-background-color: #003a2b;"
              + "-fx-text-fill: #00ff88;"
              + "-fx-border-color: #00ff88;"
              + "-fx-background-radius: 6;"
              + "-fx-border-radius: 6;");
      if (lblStatus != null) {
        lblStatus.setStyle("-fx-text-fill: #00ff88; -fx-font-size: 18px; -fx-font-weight: bold;");
      }

      matrixThemeApplied = true;
    } else {
      if (!matrixThemeApplied) return;
      txtaChat.setStyle("");
      txtInput.setStyle("");
      btnSend.setStyle("");
      if (lblStatus != null) lblStatus.setStyle("");
      matrixThemeApplied = false;
    }
  }

  // =======================
  // == AUDIT LOG (SIMPLE) ==
  // =======================

  /** One row in the simple audit log. */
  public static class AuditRow {
    private final SimpleStringProperty time = new SimpleStringProperty();
    private final SimpleStringProperty site = new SimpleStringProperty();
    private final SimpleStringProperty action = new SimpleStringProperty();
    private final SimpleStringProperty allowed = new SimpleStringProperty();

    public AuditRow(String time, String site, String action, String allowed) {
      this.time.set(time);
      this.site.set(site);
      this.action.set(action);
      this.allowed.set(allowed);
    }

    public String getTime() {
      return time.get();
    }

    public String getSite() {
      return site.get();
    }

    public String getAction() {
      return action.get();
    }

    public String getAllowed() {
      return allowed.get();
    }

    public SimpleStringProperty timeProperty() {
      return time;
    }

    public SimpleStringProperty siteProperty() {
      return site;
    }

    public SimpleStringProperty actionProperty() {
      return action;
    }

    public SimpleStringProperty allowedProperty() {
      return allowed;
    }
  }

  private void initAuditTable() {
    if (auditTable == null) return;
    if (colTime != null) colTime.setCellValueFactory(c -> c.getValue().timeProperty());
    if (colSite != null) colSite.setCellValueFactory(c -> c.getValue().siteProperty());
    if (colAction != null) colAction.setCellValueFactory(c -> c.getValue().actionProperty());
    if (colAllowed != null) colAllowed.setCellValueFactory(c -> c.getValue().allowedProperty());

    auditFiltered = new FilteredList<>(auditMaster, r -> true);
    auditTable.setItems(auditFiltered);
  }

  // Button: Load sample rows
  @FXML
  private void onLoadSample() {
    auditMaster.clear();
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Human blocked
    auditMaster.add(
        new AuditRow(
            LocalTime.now().minusSeconds(75).format(fmt),
            "Cornhub.com",
            "Share precise location",
            "No (user blocked)"));
    auditMaster.add(
        new AuditRow(
            LocalTime.now().minusSeconds(60).format(fmt),
            "NZHerald.com",
            "Marketing cookies",
            "No (user blocked)"));

    // Gym video → supplement ad (tracking without consent)
    auditMaster.add(
        new AuditRow(
            LocalTime.now().minusSeconds(45).format(fmt),
            "Youtube.com",
            "Watched gym recovery video",
            "Yes"));
    auditMaster.add(
        new AuditRow(
            LocalTime.now().minusSeconds(35).format(fmt),
            "Susweb.com",
            "Cross-site tracking",
            "Yes (no consent)"));
    auditMaster.add(
        new AuditRow(
            LocalTime.now().minusSeconds(25).format(fmt),
            "Goldsgym.com",
            "Targeted ad: supplements",
            "Yes (no consent)"));

    // Neutral conveniences
    auditMaster.add(
        new AuditRow(
            LocalTime.now().minusSeconds(15).format(fmt),
            "Woolworths.com",
            "Remember cart",
            "Yes"));
    auditMaster.add(
        new AuditRow(
            LocalTime.now().minusSeconds(8).format(fmt), "Twitch.com", "Save volume", "Yes"));

    setAuditStatus("Loaded: 7");

    // 1) Put each row into global AWARENESS so personas can quote exact lines
    syncAwarenessWithAuditRows();

    // 2) Broadcast summary and every row verbatim
    postToBusAndChat("[AWARENESS] Loaded sample audit events (Sentri-Guard).");
    broadcastAuditRows();

    // concise one-liner context
    broadcastMemorySummary(
        Participant.AI_WITNESS,
        "Tracking chain: watched gym recovery video → cross-site tracking → targeted supplement"
            + " ads; some items blocked by human, others allowed without consent.");
    
    // Reveal AI witness hint after loading audit log
    revealAiWitnessHint();
  }

  @FXML
  private void onFilterAllowed() {
    if (auditFiltered == null) return;
    auditFiltered.setPredicate(r -> r.getAllowed().toLowerCase().startsWith("yes"));
    setAuditStatus("Allowed");
  }

  @FXML
  private void onFilterBlocked() {
    if (auditFiltered == null) return;
    auditFiltered.setPredicate(r -> r.getAllowed().toLowerCase().startsWith("no"));
    setAuditStatus("Blocked");
  }

  @FXML
  private void onResetFilters() {
    if (auditFiltered == null) return;
    auditFiltered.setPredicate(r -> true);
    setAuditStatus("All");
  }

  @FXML
  private void onExplainSelected() {
    if (auditTable == null || txtaChat == null) return;
    AuditRow sel = auditTable.getSelectionModel().getSelectedItem();
    if (sel == null) {
      txtaChat.appendText("[SYSTEM] Pick a row.\n\n");
      return;
    }
    boolean noConsent = sel.getAllowed().toLowerCase().contains("no consent");
    boolean userBlocked = sel.getAllowed().toLowerCase().contains("user blocked");
    String why =
        noConsent
            ? "— allowed WITHOUT consent by Cookie Monster"
            : userBlocked ? "— blocked by the user" : "— allowed (site convenience)";
    txtaChat.appendText(
        "[SYSTEM] " + sel.getSite() + " → " + sel.getAction() + " " + why + ".\n\n");

    // broadcast which row was explained
    broadcastMemorySummary(
        Participant.AI_WITNESS,
        "Explained: " + sel.getSite() + " — " + sel.getAction() + " (" + sel.getAllowed() + ").");
  }

  @FXML
  private void onCloseAudit() {
    if (auditPanel != null) {
      auditPanel.setVisible(false);
      auditPanel.setManaged(false);
    }
    if (interactableContainer != null) interactableContainer.setVisible(true);
  }

  private void setAuditStatus(String text) {
    if (auditStatus != null) auditStatus.setText(text);
  }
}
