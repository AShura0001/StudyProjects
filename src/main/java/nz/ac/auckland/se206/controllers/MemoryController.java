package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

  // State tracking
  private boolean interactableUsed = false;

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
    // Update cookie consent visibility when game state is set
    updateCookieConsentVisibility();
  }

  public void setCurrentParticipant(Participant participant) {
    this.currentParticipant = participant;
    updateMemoryContext();
    updateCookieConsentVisibility();
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

  /**
   * Updates cookie consent button visibility based on current state.
   */
  private void updateCookieConsentVisibility() {
    // Only show cookie consent for defendant (Cookie Monster)
    if (currentParticipant == Participant.DEFENDANT && gameStateContext != null) {
      boolean cookiesAccepted = gameStateContext.isCookiesAccepted();
      
      // Hide/show cookie consent buttons based on acceptance state
      if (btnAccept != null) {
        btnAccept.setVisible(!cookiesAccepted);
      }
      if (btnExit != null) {
        btnExit.setVisible(!cookiesAccepted);
      }
      
      // Show/hide "ALL COOKIES ACCEPTED" label
      if (lblCookiesAccepted != null) {
        lblCookiesAccepted.setVisible(cookiesAccepted);
      }
      
      // Control text input and send button based on cookie acceptance
      if (txtInput != null) {
        txtInput.setDisable(!cookiesAccepted);
        if (!cookiesAccepted) {
          txtInput.setPromptText("Accept cookies first to type in");
        } else {
          txtInput.setPromptText("Type your message here...");
        }
      }
      if (btnSend != null) {
        btnSend.setDisable(!cookiesAccepted);
      }
      
      // If cookies are already accepted, request opening message
      if (cookiesAccepted) {
        requestOpeningMessage();
      }
    } else {
      // For non-defendant participants, hide cookie buttons and labels
      if (btnAccept != null) {
        btnAccept.setVisible(false);
      }
      if (btnExit != null) {
        btnExit.setVisible(false);
      }
      if (lblCookiesAccepted != null) {
        lblCookiesAccepted.setVisible(false);
      }
      
      // Enable chat for other participants
      if (txtInput != null) {
        txtInput.setDisable(false);
        txtInput.setPromptText("Type your message here...");
      }
      if (btnSend != null) {
        btnSend.setDisable(false);
      }
      
      // Request opening message for other participants
      if (currentParticipant != null) {
        requestOpeningMessage();
      }
    }
  }

  /**
   * Request opening message from the current participant.
   */
  private void requestOpeningMessage() {
    if (gameStateContext != null && gameStateContext.getChatController() != null && currentParticipant != null) {
      // Mark participant as contacted so subsequent clicks go directly to memory
      gameStateContext.markParticipantContacted(currentParticipant);
      
      gameStateContext.getChatController().selectParticipant(currentParticipant);
      gameStateContext.getChatController().requestOpeningMessageFromLlm(currentParticipant);
    }
  }

  /**
   * Handles accepting cookies for the defendant memory scene.
   */
  @FXML
  private void onAcceptCookies() {
    // Mark that the defendant has accepted cookies
    gameStateContext.acceptCookies();
    
    // Hide the cookie consent buttons
    if (btnAccept != null) {
      btnAccept.setVisible(false);
    }
    if (btnExit != null) {
      btnExit.setVisible(false);
    }
    
    // Show "ALL COOKIES ACCEPTED" label
    if (lblCookiesAccepted != null) {
      lblCookiesAccepted.setVisible(true);
    }
    
    // Enable chat input and send button
    if (txtInput != null) {
      txtInput.setDisable(false);
      txtInput.setPromptText("Type your message here...");
    }
    if (btnSend != null) {
      btnSend.setDisable(false);
    }
    
    // Request opening message from Cookie Monster
    requestOpeningMessage();
  }

  /**
   * Handles declining cookies and exiting the defendant memory scene.
   */
  @FXML
  private void onDeclineCookies() {
    // Don't set cookies as accepted
    // Return to room
    if (parent != null) {
      parent.backToRoom();
    }
  }
}
