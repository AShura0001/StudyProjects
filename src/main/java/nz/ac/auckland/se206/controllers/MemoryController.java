package nz.ac.auckland.se206.controllers;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import nz.ac.auckland.se206.GameStateContext;
import nz.ac.auckland.se206.controllers.ChatController.Participant;
import nz.ac.auckland.se206.persona.AwarenessFormatter; // push rows/memory into AWARENESS
import nz.ac.auckland.se206.persona.ConversationBus;      // awareness broadcast bus

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

  // Cookie consent buttons (for defendant memory)
  @FXML private Button btnAccept;
  @FXML private Button btnExit;
  @FXML private Label lblCookiesAccepted;

  // ===== Human witness: Frustration mini-game (supports BOTH UIs) =====
  @FXML private VBox frustrationSection;
  @FXML private Slider frustrationSlider;
  @FXML private Label frustrationValueLabel;

  // Style A: "Higher" / "Lower" buttons
  @FXML private Button btnHigher;
  @FXML private Button btnLower;
  @FXML private VBox frustrationDialogue;
  @FXML private Label frustrationSpeaker; // may be unused, present in some FXMLs
  @FXML private Label frustrationDialogueText;

  // Style B: single "Guess Level" button + context reveal
  @FXML private Button btnUnderstandStory;
  @FXML private VBox contextRevelation;
  @FXML private Label contextDetails;

  // ===== AI witness: extra controls (from second file) =====
  @FXML private Button btnAnalyzeMemory;
  @FXML private Button btnValidateMemory;
  @FXML private VBox memoryLog;

  // ---- Simple Audit Log (AI Witness) ---- (from first file)
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
  private int targetFrustrationLevel = 8; // unify on 8; both UIs use shared logic
  private boolean frustrationGuessed = false;

  // Theme tracking (Matrix theme for AI_WITNESS)
  private boolean matrixThemeApplied = false;

  public void setParent(RoomFrameController parent) {
    this.parent = parent;
  }

  public void setGameStateContext(GameStateContext gameStateContext) {
    this.gameStateContext = gameStateContext;
    if (gameStateContext != null && gameStateContext.getChatController() != null) {
      gameStateContext.getChatController().setFlashbackChatComponents(lblStatus, txtaChat, txtInput, btnSend);
    }
    enableChatAutoScroll();
  }

  public void setCurrentParticipant(Participant participant) {
    this.currentParticipant = participant;
    updateMemoryContext();
    updateCookieConsentVisibility();
    updateFrustrationSectionVisibility();
    updateAiWitnessButtonState();
    applyThemeForParticipant();
  }

  @FXML
  private void initialize() {
    if (interactableHighlight != null) {
      interactableHighlight.setVisible(false);
      interactableHighlight.setMouseTransparent(true); // overlay never steals clicks
    }
    if (interactableElement != null) {
      interactableElement.setPickOnBounds(true);
      interactableElement.setOnMouseEntered(e -> showInteractableHighlight());
      interactableElement.setOnMouseExited(e -> hideInteractableHighlight());
    }
    if (frustrationSlider != null && frustrationValueLabel != null) {
      frustrationSlider.valueProperty().addListener(
        (obs, oldVal, newVal) -> frustrationValueLabel.setText(String.valueOf(newVal.intValue()))
      );
    }
    enableChatAutoScroll();
    initAuditTable();
  }

  // ---------------- Navigation ----------------
  @FXML
  private void onBackToRoom() {
    if (parent != null) parent.backToRoom();
  }

  // ---------------- Chat send ----------------
  @FXML
  private void onSendMessage() {
    if (currentParticipant == Participant.DEFENDANT) {
      if (gameStateContext != null && gameStateContext.isCookiesAccepted() && gameStateContext.getChatController() != null) {
        gameStateContext.getChatController().onSendMessage();
      }
    } else {
      if (gameStateContext != null && gameStateContext.getChatController() != null) {
        gameStateContext.getChatController().onSendMessage();
      }
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
        auditPanel.toFront();
      }
      setAuditStatus("Tap Load");

      // Awareness + broadcast
      AwarenessFormatter.addMemoryEvent("ai_witness", "Opened Simple Audit Log");
      broadcastAuditOpenedAwareness();
    }

    notifyChatAboutInteraction();
    updateAiWitnessButtonState();
  }

  /** Broadcast a room-wide awareness line so all personas know this memory was opened. */
  private void broadcastAuditOpenedAwareness() {
    String msg =
        "[AWARENESS] Memory used → AI Witness (Sentri-Guard): Simple Audit Log opened; "
            + "shows ALLOWED (no consent) vs BLOCKED (human) events, including a path "
            + "‘watched gym video → cross-site tracking → targeted supplement ads’.";
    postToBusAndChat(msg);
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
      // If the bus isn't available in a given test harness, we still show it in the chat.
    }
    if (txtaChat != null) {
      txtaChat.appendText(message + "\n");
    }
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
        auditPanel.toFront();
      }
    }

    updateInteractableState();
  }

  private String getRoleKeyOf(Participant participant) {
    if (participant == null) return "ai_defendant";
    switch (participant) {
      case HUMAN_WITNESS: return "human_witness";
      case AI_WITNESS:    return "ai_witness";
      case DEFENDANT:
      default:            return "ai_defendant";
    }
  }

  private void updateInteractableState() {
    if (interactableElement != null && interactableLabel != null) {
      if (interactableUsed) {
        interactableElement.setDisable(true);
        interactableElement.setStyle("-fx-opacity: 0.6;");
        // Choose a neutral label that fits both scenes
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

  // ---------------- Cookie gating (Defendant) ----------------
  private void updateCookieConsentVisibility() {
    if (currentParticipant == Participant.DEFENDANT && gameStateContext != null) {
      boolean cookiesAccepted = gameStateContext.isCookiesAccepted();

      if (btnAccept != null) btnAccept.setVisible(!cookiesAccepted);
      if (btnExit != null) btnExit.setVisible(!cookiesAccepted);
      if (lblCookiesAccepted != null) lblCookiesAccepted.setVisible(cookiesAccepted);

      if (txtInput != null) {
        txtInput.setDisable(!cookiesAccepted);
        txtInput.setPromptText(cookiesAccepted ? "Type your message here..." : "Accept cookies first to type in");
      }
      if (btnSend != null) btnSend.setDisable(!cookiesAccepted);
    } else {
      if (btnAccept != null) btnAccept.setVisible(false);
      if (btnExit != null) btnExit.setVisible(false);
      if (lblCookiesAccepted != null) lblCookiesAccepted.setVisible(false);

      if (txtInput != null) {
        txtInput.setDisable(false);
        txtInput.setPromptText("Type your message here...");
      }
      if (btnSend != null) btnSend.setDisable(false);

      if (currentParticipant != null) {
        requestOpeningMessage();
      }
    }
  }

  private void requestOpeningMessage() {
    if (gameStateContext != null
        && gameStateContext.getChatController() != null
        && currentParticipant != null) {
      gameStateContext.markParticipantContacted(currentParticipant);
      gameStateContext.getChatController().selectParticipant(currentParticipant);
      gameStateContext.getChatController().requestOpeningMessageFromLlm(currentParticipant);
    }
  }

  @FXML
  private void onAcceptCookies() {
    gameStateContext.acceptCookies();

    if (btnAccept != null) btnAccept.setVisible(false);
    if (btnExit != null) btnExit.setVisible(false);
    if (lblCookiesAccepted != null) lblCookiesAccepted.setVisible(true);

    if (txtInput != null) {
      txtInput.setDisable(false);
      txtInput.setPromptText("Type your message here...");
    }
    if (btnSend != null) btnSend.setDisable(false);

    requestOpeningMessage();
  }

  @FXML
  private void onDeclineCookies() {
    if (parent != null) parent.backToRoom();
  }

  // ---------------- Human frustration mini-game ----------------
  // Style A buttons
  @FXML
  private void onFrustrationHigher() {
    if (frustrationGuessed) return;
    int currentGuess = (int) (frustrationSlider != null ? frustrationSlider.getValue() : targetFrustrationLevel);
    evaluateFrustrationGuess(currentGuess, /*fromButtons=*/true);
  }

  @FXML
  private void onFrustrationLower() {
    if (frustrationGuessed) return;
    int currentGuess = (int) (frustrationSlider != null ? frustrationSlider.getValue() : targetFrustrationLevel);
    evaluateFrustrationGuess(currentGuess, /*fromButtons=*/true);
  }

  // Style B single button
  @FXML
  private void onUnderstandStory() {
    if (frustrationGuessed) return;
    int currentGuess = (int) (frustrationSlider != null ? frustrationSlider.getValue() : targetFrustrationLevel);
    evaluateFrustrationGuess(currentGuess, /*fromButtons=*/false);
  }

  /** Shared logic for both UIs. */
  private void evaluateFrustrationGuess(int guess, boolean fromButtons) {
    if (guess == targetFrustrationLevel) {
      onCorrectFrustrationGuess();
    } else {
      String hint =
          (guess < targetFrustrationLevel)
              ? "I'm MORE frustrated than " + guess + " out of 10. This privacy violation really hurt me. Try higher."
              : "I'm upset but not quite " + guess + " out of 10. Try slightly lower.";
      showFrustrationDialogue(hint);
      // Keep Style B button enabled; Style A will continue offering Higher/Lower.
    }
  }

  private void onCorrectFrustrationGuess() {
    frustrationGuessed = true;

    // Style A: disable Higher/Lower
    if (btnHigher != null) {
      btnHigher.setDisable(true);
      btnHigher.setStyle("-fx-opacity: 0.5; -fx-background-color: #cccccc;");
    }
    if (btnLower != null) {
      btnLower.setDisable(true);
      btnLower.setStyle("-fx-opacity: 0.5; -fx-background-color: #cccccc;");
    }

    // Style B: update Guess button
    if (btnUnderstandStory != null) {
      btnUnderstandStory.setDisable(true);
      btnUnderstandStory.setText("Correct!");
      btnUnderstandStory.setStyle("-fx-opacity: 0.6; -fx-background-color: #4caf50;");
    }

    // Narrative + context
    String successResponse =
        "EXACTLY! I'm at an "
            + targetFrustrationLevel
            + " out of 10! I searched for gym training programs and supplements, then got targeted ads everywhere. I never consented to this tracking!";
    showFrustrationDialogue(successResponse);

    String contextualEvidence =
        "Casey searched privately for gym training programs and fitness supplements. Within 48 hours, "
            + "supplement and protein powder ads appeared on every website he visited. Cookie Monster "
            + "auto-accepted tracking cookies without his consent—he never chose 'Accept All.' This shows the "
            + "defendant prioritized convenience over informed consent.";
    showContextualEvidence(contextualEvidence);

    if (gameStateContext != null && currentParticipant == Participant.HUMAN_WITNESS) {
      gameStateContext.markInteractableDone(getRoleKeyOf(currentParticipant));
      interactableUsed = true;
    }
  }

  private void showFrustrationDialogue(String dialogueText) {
    if (frustrationDialogue != null && frustrationDialogueText != null) {
      frustrationDialogueText.setText(dialogueText);
      frustrationDialogue.setVisible(true);
    }
  }

  private void showContextualEvidence(String evidenceText) {
    if (contextRevelation != null && contextDetails != null) {
      contextDetails.setText(evidenceText);
      contextRevelation.setVisible(true);
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
      if (btnHigher != null) { btnHigher.setDisable(true); btnHigher.setStyle("-fx-opacity: 0.5; -fx-background-color: #cccccc;"); }
      if (btnLower != null) { btnLower.setDisable(true); btnLower.setStyle("-fx-opacity: 0.5; -fx-background-color: #cccccc;"); }
      if (btnUnderstandStory != null) {
        btnUnderstandStory.setDisable(true);
        btnUnderstandStory.setText("Correct!");
        btnUnderstandStory.setStyle("-fx-opacity: 0.6; -fx-background-color: #4caf50;");
      }
      if (frustrationSlider != null) frustrationSlider.setValue(targetFrustrationLevel);
      // Re-show texts
      showFrustrationDialogue(
        "EXACTLY! I'm at an " + targetFrustrationLevel + " out of 10! I searched for gym training programs and supplements, then got targeted ads everywhere. I never consented to this tracking!"
      );
      showContextualEvidence(
        "Casey searched privately for gym training programs and fitness supplements. Within 48 hours, "
        + "supplement and protein powder ads appeared on every website he visited. Cookie Monster auto-accepted "
        + "tracking cookies without his consent—he never chose 'Accept All.' This shows the defendant prioritized "
        + "convenience over informed consent."
      );
    } else {
      if (frustrationDialogue != null) frustrationDialogue.setVisible(false);
      if (contextRevelation != null) contextRevelation.setVisible(false);
      if (btnUnderstandStory != null) {
        btnUnderstandStory.setDisable(false);
        btnUnderstandStory.setText("🎯 Guess Level");
        btnUnderstandStory.setStyle(
            "-fx-font-size: 16px; -fx-background-color: #ff9800; -fx-text-fill: white;"
                + " -fx-pref-width: 150; -fx-pref-height: 45; -fx-border-radius: 8; -fx-background-radius: 8;");
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
            "-fx-font-size: 14px; -fx-background-color: #ffebcd; -fx-text-fill: #222; -fx-pref-width: 120;");
      }
      if (btnValidateMemory != null) {
        btnValidateMemory.setDisable(false);
        btnValidateMemory.setText("Validate");
        btnValidateMemory.setStyle(
            "-fx-font-size: 14px; -fx-background-color: #f0e68c; -fx-text-fill: #222; -fx-pref-width: 120;");
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
    if (gameStateContext != null && gameStateContext.getChatController() != null && txtaChat != null) {
      String interactionMessage =
          "[SYSTEM] You "
              + action.toLowerCase()
              + "d the AI witness's memory data analysis core, accessing detailed incident logs and processing records...";
      txtaChat.appendText(interactionMessage + "\n\n");

      if (gameStateContext.isFirstInteraction(currentParticipant)) {
        requestOpeningMessage();
      }
    }
  }

  // ---------------- Chat helpers ----------------
  private void enableChatAutoScroll() {
    if (txtaChat == null) return;
    if (txtaChat.getProperties().get("autoscrollBound") == Boolean.TRUE) return;

    txtaChat.textProperty().addListener((obs, oldText, newText) -> {
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

    public String getTime() { return time.get(); }
    public String getSite() { return site.get(); }
    public String getAction() { return action.get(); }
    public String getAllowed() { return allowed.get(); }

    public SimpleStringProperty timeProperty() { return time; }
    public SimpleStringProperty siteProperty() { return site; }
    public SimpleStringProperty actionProperty() { return action; }
    public SimpleStringProperty allowedProperty() { return allowed; }
  }

  private void initAuditTable() {
    if (auditTable == null) return;

    if (colTime != null)   colTime.setCellValueFactory(c -> c.getValue().timeProperty());
    if (colSite != null)   colSite.setCellValueFactory(c -> c.getValue().siteProperty());
    if (colAction != null) colAction.setCellValueFactory(c -> c.getValue().actionProperty());
    if (colAllowed != null)colAllowed.setCellValueFactory(c -> c.getValue().allowedProperty());

    auditFiltered = new FilteredList<>(auditMaster, r -> true);
    auditTable.setItems(auditFiltered);
  }

  // Button: Load sample rows (includes "no consent", human blocked, and gym→ad story)
  @FXML
  private void onLoadSample() {
    auditMaster.clear();
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Human blocked
    auditMaster.add(new AuditRow(LocalTime.now().minusSeconds(75).format(fmt), "Cornhub.com", "Share precise location", "No (user blocked)"));
    auditMaster.add(new AuditRow(LocalTime.now().minusSeconds(60).format(fmt), "NZHerald.com", "Marketing cookies", "No (user blocked)"));

    // Gym video → supplement ad (tracking without consent)
    auditMaster.add(new AuditRow(LocalTime.now().minusSeconds(45).format(fmt), "Youtube.com", "Watched gym recovery video", "Yes"));
    auditMaster.add(new AuditRow(LocalTime.now().minusSeconds(35).format(fmt), "Susweb.com", "Cross-site tracking", "Yes (no consent)"));
    auditMaster.add(new AuditRow(LocalTime.now().minusSeconds(25).format(fmt), "Goldsgym.com", "Targeted ad: supplements", "Yes (no consent)"));

    // Neutral conveniences
    auditMaster.add(new AuditRow(LocalTime.now().minusSeconds(15).format(fmt), "Woolworths.com", "Remember cart", "Yes"));
    auditMaster.add(new AuditRow(LocalTime.now().minusSeconds(8).format(fmt), "Twitch.com", "Save volume", "Yes"));

    setAuditStatus("Loaded: 7");

    // 1) Put each row into global AWARENESS so personas can quote exact lines
    syncAwarenessWithAuditRows();

    // 2) Broadcast summary and every row verbatim
    postToBusAndChat("[AWARENESS] Loaded sample audit events (Sentri-Guard).");
    broadcastAuditRows();
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
    txtaChat.appendText("[SYSTEM] " + sel.getSite() + " → " + sel.getAction() + " " + why + ".\n\n");
  }

  @FXML
  private void onCloseAudit() {
    if (auditPanel != null) auditPanel.setVisible(false);
    if (interactableContainer != null) interactableContainer.setVisible(true);
  }

  private void setAuditStatus(String text) {
    if (auditStatus != null) auditStatus.setText(text);
  }
}

