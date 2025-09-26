package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import nz.ac.auckland.se206.GameStateContext;
import nz.ac.auckland.se206.controllers.ChatController.Participant;

/**
 * Controller for memory scenes where players interact with participants in their memory spaces with
 * unique interactable elements.
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

  // Frustration level components (for human witness memory)
  @FXML private VBox frustrationSection;
  @FXML private Slider frustrationSlider;
  @FXML private Label frustrationValueLabel;
  @FXML private Button btnUnderstandStory;
  @FXML private VBox frustrationDialogue;
  @FXML private Label frustrationDialogueText;
  @FXML private VBox contextRevelation;
  @FXML private Label contextDetails;

  // AI witness memory components
  @FXML private Button btnAnalyzeMemory;
  @FXML private Button btnValidateMemory;
  @FXML private VBox memoryLog;

  // State tracking
  private boolean interactableUsed = false;
  private int targetFrustrationLevel = 8; // The correct frustration level to guess
  private boolean frustrationGuessed = false;

  public void setParent(RoomFrameController parent) {
    this.parent = parent;
  }

  public void setGameStateContext(GameStateContext gameStateContext) {
    this.gameStateContext = gameStateContext;
    if (gameStateContext != null && gameStateContext.getChatController() != null) {
      // Connect memory chat widgets to the main ChatController
      gameStateContext
          .getChatController()
          .setFlashbackChatComponents(lblStatus, txtaChat, txtInput, btnSend);
    }
    // Cookie consent visibility will be updated when setCurrentParticipant is called
  }

  public void setCurrentParticipant(Participant participant) {
    this.currentParticipant = participant;
    updateMemoryContext();
    updateCookieConsentVisibility();
    updateFrustrationSectionVisibility();
    updateAiWitnessButtonState();
  }

  @FXML
  private void initialize() {
    // Initialize UI state
    if (interactableHighlight != null) {
      interactableHighlight.setVisible(false);
    }

    // Set up interactable hover effects
    if (interactableElement != null) {
      interactableElement.setOnMouseEntered(e -> showInteractableHighlight());
      interactableElement.setOnMouseExited(e -> hideInteractableHighlight());
    }

    // Set up frustration slider
    if (frustrationSlider != null && frustrationValueLabel != null) {
      frustrationSlider
          .valueProperty()
          .addListener(
              (obs, oldVal, newVal) -> {
                frustrationValueLabel.setText(String.valueOf(newVal.intValue()));
              });
    }
  }

  @FXML
  private void onBackToRoom() {
    if (parent != null) {
      parent.backToRoom();
    }
  }

  @FXML
  private void onSendMessage() {
    // For defendant (Cookie Monster), only allow chat if cookies are accepted
    if (currentParticipant == Participant.DEFENDANT) {
      if (gameStateContext != null
          && gameStateContext.isCookiesAccepted()
          && gameStateContext.getChatController() != null) {
        gameStateContext.getChatController().onSendMessage();
      }
    } else {
      // For other participants, allow chat normally
      if (gameStateContext != null && gameStateContext.getChatController() != null) {
        gameStateContext.getChatController().onSendMessage();
      }
    }
  }

  @FXML
  private void onInteractableClick() {
    if (gameStateContext == null || currentParticipant == null) {
      return;
    }

    String roleKey = getRoleKeyOf(currentParticipant);

    // Mark interactable as used
    gameStateContext.markInteractableDone(roleKey);
    interactableUsed = true;

    // Update UI to show interaction happened
    updateInteractableState();

    // Notify chat system about the interaction
    notifyChatAboutInteraction();

    System.out.println("DEBUG: Interactable used for " + roleKey);
  }

  private void updateMemoryContext() {
    if (gameStateContext == null || currentParticipant == null) {
      return;
    }

    String roleKey = getRoleKeyOf(currentParticipant);
    interactableUsed = gameStateContext.getInteractableDone().getOrDefault(roleKey, false);

    // For human witness, check if frustration was already guessed
    if (currentParticipant == Participant.HUMAN_WITNESS) {
      frustrationGuessed = interactableUsed;
      updateFrustrationButtonState();
    }

    // For AI witness, update button states
    if (currentParticipant == Participant.AI_WITNESS) {
      updateAiWitnessButtonState();
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
        return "ai_defendant";
      default:
        return "ai_defendant";
    }
  }

  private void updateInteractableState() {
    if (interactableElement != null && interactableLabel != null) {
      if (interactableUsed) {
        interactableElement.setDisable(true);
        interactableElement.setStyle("-fx-opacity: 0.6;");
        interactableLabel.setText("Already examined");
      } else {
        interactableElement.setDisable(false);
        interactableElement.setStyle("-fx-opacity: 1.0;");
        interactableLabel.setText("Click to examine");
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
    // Send a contextual message about the interaction
    if (gameStateContext != null
        && gameStateContext.getChatController() != null
        && currentParticipant != null) {
      String interactionMessage = getInteractionMessage();

      // Add a system message to indicate the interaction happened
      if (txtaChat != null) {
        String currentText = txtaChat.getText();
        String systemMessage = "[SYSTEM] " + interactionMessage + "\n\n";
        txtaChat.setText(currentText + systemMessage);
      }

      System.out.println(
          "DEBUG: Player interacted with " + getInteractableName() + " for " + currentParticipant);
    }
  }

  private String getInteractionMessage() {
    switch (currentParticipant) {
      case DEFENDANT:
        return "You carefully examine the defendant's private journal, reading their personal"
            + " thoughts about the incident...";
      case AI_WITNESS:
        return "You access the AI witness's data analysis core, reviewing the incident data and"
            + " processing logs...";
      case HUMAN_WITNESS:
        return "You open the witness evidence collection, examining the physical items they"
            + " gathered from the scene...";
      default:
        return "You examine the interactable element in this memory space...";
    }
  }

  private String getInteractableName() {
    switch (currentParticipant) {
      case DEFENDANT:
        return "Private Journal";
      case AI_WITNESS:
        return "Data Analysis Core";
      case HUMAN_WITNESS:
        return "Evidence Collection";
      default:
        return "Memory Object";
    }
  }

  public boolean isInteractableUsed() {
    return interactableUsed;
  }

  /** Updates cookie consent button visibility based on current state. */
  private void updateCookieConsentVisibility() {
    if (currentParticipant == Participant.DEFENDANT && gameStateContext != null) {
      boolean cookiesAccepted = gameStateContext.isCookiesAccepted();

      if (btnAccept != null) {
        btnAccept.setVisible(!cookiesAccepted);
      }
      if (btnExit != null) {
        btnExit.setVisible(!cookiesAccepted);
      }
      if (lblCookiesAccepted != null) {
        lblCookiesAccepted.setVisible(cookiesAccepted);
      }

      if (txtInput != null) {
        txtInput.setDisable(!cookiesAccepted);
        txtInput.setPromptText(
            cookiesAccepted ? "Type your message here..." : "Accept cookies first to type in");
      }
      if (btnSend != null) {
        btnSend.setDisable(!cookiesAccepted);
      }
    } else {
      // For other participants
      if (btnAccept != null) btnAccept.setVisible(false);
      if (btnExit != null) btnExit.setVisible(false);
      if (lblCookiesAccepted != null) lblCookiesAccepted.setVisible(false);

      if (txtInput != null) {
        txtInput.setDisable(false);
        txtInput.setPromptText("Type your message here...");
      }
      if (btnSend != null) btnSend.setDisable(false);

      // Keep opening message for others
      if (currentParticipant != null) {
        requestOpeningMessage();
      }
    }
  }

  /** Request opening message from the current participant. */
  private void requestOpeningMessage() {
    if (gameStateContext != null
        && gameStateContext.getChatController() != null
        && currentParticipant != null) {
      // Mark participant as contacted so subsequent clicks go directly to memory
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

    // ✅ Only here do we trigger the Defendant’s opening message once
    requestOpeningMessage();
  }

  /** Handles declining cookies and exiting the defendant memory scene. */
  @FXML
  private void onDeclineCookies() {
    // Don't set cookies as accepted
    // Return to room
    if (parent != null) {
      parent.backToRoom();
    }
  }

  /** Handles the "Guess Level" button click. */
  @FXML
  private void onUnderstandStory() {
    if (frustrationGuessed) {
      return; // Already completed
    }

    int playerGuess = (int) frustrationSlider.getValue();

    if (playerGuess == targetFrustrationLevel) {
      // Correct guess - reveal full story and context
      onCorrectGuess();
    } else {
      // Wrong guess - give hints
      giveHint(playerGuess);
    }
  }

  /** Handles when the player correctly guesses the frustration level. */
  private void onCorrectGuess() {
    frustrationGuessed = true;

    // Disable the button and update its appearance
    if (btnUnderstandStory != null) {
      btnUnderstandStory.setDisable(true);
      btnUnderstandStory.setText("Correct!");
      btnUnderstandStory.setStyle("-fx-opacity: 0.6; -fx-background-color: #4caf50;");
    }

    // Show the success response with full context
    String successResponse =
        "EXACTLY! I'm at an "
            + targetFrustrationLevel
            + " out of 10! I searched for gym training programs and supplements, then got"
            + " targeted ads everywhere. I never consented to this tracking!";

    showFrustrationDialogue(successResponse);

    // Only show extra context when the correct level is guessed
    String contextualEvidence =
        "Casey searched privately for gym training programs and fitness supplements. Within 48"
            + " hours, supplement and protein powder ads appeared on every website he visited."
            + " Cookie Monster auto-accepted tracking cookies without his consent - he never chose"
            + " 'Accept All.' This shows the defendant prioritized convenience over informed"
            + " consent.";

    showContextualEvidence(contextualEvidence);

    // Mark this interactive element as used
    if (gameStateContext != null && currentParticipant == Participant.HUMAN_WITNESS) {
      gameStateContext.markInteractableDone(getRoleKeyOf(currentParticipant));
      interactableUsed = true;
    }
  }

  /** Gives hints when the player guesses incorrectly. */
  private void giveHint(int playerGuess) {
    String hint;

    if (playerGuess < targetFrustrationLevel) {
      // Guessed too low
      hint =
          "I'm MORE frustrated than "
              + playerGuess
              + " out of 10. This privacy violation really hurt me. Try higher - imagine your"
              + " private health searches being used for ads without consent.";
    } else {
      // Guessed too high
      hint =
          "I'm upset but not quite "
              + playerGuess
              + " out of 10. I feel betrayed but haven't lost all faith. Try slightly lower.";
    }

    showFrustrationDialogue(hint);
  }

  /** Shows the frustration dialogue with the given text. */
  private void showFrustrationDialogue(String dialogueText) {
    if (frustrationDialogue != null && frustrationDialogueText != null) {
      frustrationDialogueText.setText(dialogueText);
      frustrationDialogue.setVisible(true);
    }
  }

  /** Shows the contextual evidence that can influence verdict decisions. */
  private void showContextualEvidence(String evidenceText) {
    if (contextRevelation != null && contextDetails != null) {
      contextDetails.setText(evidenceText);
      contextRevelation.setVisible(true);
    }
  }

  /** Updates the frustration section visibility based on current participant. */
  private void updateFrustrationSectionVisibility() {
    if (frustrationSection != null) {
      // Only show frustration section for human witness
      boolean showFrustration = (currentParticipant == Participant.HUMAN_WITNESS);
      frustrationSection.setVisible(showFrustration);

      if (showFrustration) {
        updateFrustrationButtonState();
      }
    }
  }

  /**
   * Updates the state of the story button and dialogue based on whether the assessment was
   * completed.
   */
  private void updateFrustrationButtonState() {
    if (frustrationGuessed) {
      // Already guessed correctly - show disabled button and keep dialogue visible
      if (btnUnderstandStory != null) {
        btnUnderstandStory.setDisable(true);
        btnUnderstandStory.setText("Correct!");
        btnUnderstandStory.setStyle("-fx-opacity: 0.6; -fx-background-color: #4caf50;");
      }

      // Keep the slider at the correct target level
      if (frustrationSlider != null) {
        frustrationSlider.setValue(targetFrustrationLevel);
      }

      // Keep dialogue and context visible if they were already shown
      if (frustrationDialogue != null) {
        frustrationDialogue.setVisible(true);
        // Restore the success response text
        if (frustrationDialogueText != null) {
          String successResponse =
              "EXACTLY! I'm at an "
                  + targetFrustrationLevel
                  + " out of 10! I searched for gym training programs and supplements, then got"
                  + " targeted ads everywhere. I never consented to this tracking!";
          frustrationDialogueText.setText(successResponse);
        }
      }
      if (contextRevelation != null) {
        contextRevelation.setVisible(true);
        // Restore the contextual evidence text
        if (contextDetails != null) {
          String contextualEvidence =
              "Casey searched privately for gym training programs and fitness supplements. Within"
                  + " 48 hours, supplement and protein powder ads appeared on every website he"
                  + " visited. Cookie Monster auto-accepted tracking cookies without his consent -"
                  + " he never chose 'Accept All.' This shows the defendant prioritized convenience"
                  + " over informed consent.";
          contextDetails.setText(contextualEvidence);
        }
      }
    } else {
      // Not guessed correctly yet - reset dialogue and enable button
      if (frustrationDialogue != null) {
        frustrationDialogue.setVisible(false);
      }
      if (contextRevelation != null) {
        contextRevelation.setVisible(false);
      }

      if (btnUnderstandStory != null) {
        btnUnderstandStory.setDisable(false);
        btnUnderstandStory.setText("🎯 Guess Level");
        btnUnderstandStory.setStyle(
            "-fx-font-size: 16px; -fx-background-color: #ff9800; -fx-text-fill: white;"
                + " -fx-pref-width: 150; -fx-pref-height: 45; -fx-border-radius: 8;"
                + " -fx-background-radius: 8;");
      }
    }
  }

  /** Updates the AI witness button states based on whether the interactable has been used. */
  private void updateAiWitnessButtonState() {
    if (currentParticipant == Participant.AI_WITNESS) {
      if (interactableUsed) {
        // Buttons already used - disable them
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
        // Reset buttons to enabled state
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
  }

  // AI Witness memory actions
  @FXML
  private void onAnalyzeMemory() {
    if (gameStateContext != null && currentParticipant == Participant.AI_WITNESS) {
      String roleKey = getRoleKeyOf(currentParticipant);

      // Mark the interactable as done
      gameStateContext.markInteractableDone(roleKey);
      interactableUsed = true;

      // Disable the buttons after use
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

      // Add interaction message to chat
      notifyAiWitnessInteraction("Analyze Data");
      System.out.println("DEBUG: AI Witness memory analysis completed");
    }
  }

  @FXML
  private void onValidateMemory() {
    if (gameStateContext != null && currentParticipant == Participant.AI_WITNESS) {
      String roleKey = getRoleKeyOf(currentParticipant);

      // Mark the interactable as done
      gameStateContext.markInteractableDone(roleKey);
      interactableUsed = true;

      // Disable the buttons after use
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

      // Add interaction message to chat
      notifyAiWitnessInteraction("Validate");
      System.out.println("DEBUG: AI Witness memory validation completed");
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

      String currentText = txtaChat.getText();
      txtaChat.setText(currentText + interactionMessage + "\n\n");

      // Request an opening message from the AI witness if they haven't been contacted yet
      if (gameStateContext.isFirstInteraction(currentParticipant)) {
        requestOpeningMessage();
      }
    }
  }
}
