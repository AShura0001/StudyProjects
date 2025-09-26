package nz.ac.auckland.se206.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
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

  // State tracking
  private boolean interactableUsed = false;
  private int targetFrustrationLevel = 8; // The correct frustration level to guess
  private boolean frustrationGuessed = false;

  // Aim training game state
  private int gameScore = 0;
  private int totalMisses = 0;
  private boolean gameRunning = false;
  private Timeline gameTimer;
  private Timeline targetSpawnTimer;
  private List<Button> activeTargets = new ArrayList<>();
  private Random random = new Random();
  private long gameStartTime = 0;

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
    updateFrustrationSectionVisibility();
    updateAiWitnessButtonState();
    updateAimTrainingGameVisibility();
    updateChatControlsState();
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
    // Check if interactive element has been completed before allowing chat
    if (currentParticipant == Participant.DEFENDANT && !interactableUsed) {
      // Provide feedback to user that they need to complete the aim training first
      if (txtaChat != null) {
        String restrictionMessage = "\n🔒 System: Complete Cookie Monster's aim training before chatting.\n";
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

  /** Updates aim training game visibility based on current participant */
  private void updateAimTrainingGameVisibility() {
    if (aimTrainingGame != null) {
      // Only show for defendant (Cookie Monster) and if not already completed
      boolean showGame = (currentParticipant == Participant.DEFENDANT && !interactableUsed);
      aimTrainingGame.setVisible(showGame);
      
      // Request opening message for defendant when the game is shown
      if (showGame && gameStateContext != null) {
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
        String lockMessage = "\n🔒 System: Complete Cookie Monster's aim training to unlock chat functionality.\n";
        if (!currentText.contains("Complete Cookie Monster's aim training to unlock")) {
          txtaChat.setText(currentText + lockMessage);
        }
      }
    } else {
      // For other participants, enable chat normally
      if (txtInput != null) {
        txtInput.setDisable(false);
        txtInput.setPromptText("Type a message...");
      }
      if (btnSend != null) {
        btnSend.setDisable(false);
      }
    }
  }

  /** Starts the aim training mini-game */
  @FXML
  private void onStartAimGame() {
    if (gameRunning) {
      return;
    }

    // Reset game state
    gameScore = 0;
    totalMisses = 0;
    gameRunning = true;
    gameStartTime = System.currentTimeMillis();
    activeTargets.clear();

    // Update UI
    updateGameStats();
    lblGameStatus.setText("Training in progress... Click the Accept buttons!");
    btnStartGame.setText("Training Active");
    btnStartGame.setDisable(true);
    gamePlayArea.setVisible(true);
    gameResults.setVisible(false);

    // Start the game timer (updates every second)
    gameTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateGameTimer()));
    gameTimer.setCycleCount(Timeline.INDEFINITE);
    gameTimer.play();

    // Start spawning targets
    startTargetSpawning();
  }

  /** Starts the target spawning system */
  private void startTargetSpawning() {
    System.out.println("DEBUG: Starting target spawning system");
    targetSpawnTimer = new Timeline(new KeyFrame(Duration.millis(800 + random.nextInt(1200)), e -> spawnTarget()));
    targetSpawnTimer.setCycleCount(Timeline.INDEFINITE);
    targetSpawnTimer.play();
    
    // Spawn the first target immediately to test
    Platform.runLater(() -> spawnTarget());
  }

  /** Spawns a new target button at a random location */
  private void spawnTarget() {
    if (!gameRunning || gamePlayArea == null) {
      return;
    }

    // Create new target button
    Button target = new Button("Accept");
    target.setStyle(
        "-fx-font-size: 12px; -fx-background-color: #ffe066; -fx-text-fill: #222; "
        + "-fx-pref-width: 80; -fx-pref-height: 25; -fx-font-weight: bold; "
        + "-fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");

    // Use actual dimensions or fallback to pref dimensions
    double areaWidth = gamePlayArea.getWidth() > 0 ? gamePlayArea.getWidth() : gamePlayArea.getPrefWidth();
    double areaHeight = gamePlayArea.getHeight() > 0 ? gamePlayArea.getHeight() : gamePlayArea.getPrefHeight();
    
    // Random position within the play area (with some margin)
    double maxX = Math.max(0, areaWidth - 90); // button width + margin
    double maxY = Math.max(0, areaHeight - 35); // button height + margin
    double x = random.nextDouble() * maxX + 5; // 5px margin from edge
    double y = random.nextDouble() * maxY + 5; // 5px margin from edge

    target.setLayoutX(x);
    target.setLayoutY(y);

    // Add click handler
    target.setOnAction(e -> onTargetHit(target));

    // Add to play area and active targets list
    gamePlayArea.getChildren().add(target);
    activeTargets.add(target);

    // Debug output
    System.out.println("DEBUG: Spawned target at (" + x + ", " + y + ") in area " + areaWidth + "x" + areaHeight);

    // Set target to disappear after 0.7 seconds (faster for increased difficulty)
    Timeline disappearTimer = new Timeline(new KeyFrame(Duration.seconds(0.7), e -> onTargetMiss(target)));
    disappearTimer.play();
  }

  /** Handles when a target is successfully hit */
  private void onTargetHit(Button target) {
    if (!gameRunning) {
      return;
    }

    // Remove target
    gamePlayArea.getChildren().remove(target);
    activeTargets.remove(target);

    // Update score
    gameScore++;
    // Note: totalMisses is NOT reset on successful hit (unlike consecutive misses)

    updateGameStats();

    // Check win condition
    if (gameScore >= 10) {
      endGame(true);
    }
  }

  /** Handles when a target is missed (disappears) */
  private void onTargetMiss(Button target) {
    if (!gameRunning || !activeTargets.contains(target)) {
      return;
    }

    // Remove target
    gamePlayArea.getChildren().remove(target);
    activeTargets.remove(target);

    // Update miss counter
    totalMisses++;
    updateGameStats();

    // Check lose condition
    if (totalMisses >= 3) {
      endGame(false);
    }
  }

  /** Updates the game statistics display */
  private void updateGameStats() {
    if (lblScore != null) {
      lblScore.setText(gameScore + "/10");
    }
    if (lblMisses != null) {
      lblMisses.setText(totalMisses + "/3");
      // Change color based on miss count
      if (totalMisses == 0) {
        lblMisses.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #059669;");
      } else if (totalMisses == 1) {
        lblMisses.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
      } else {
        lblMisses.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
      }
    }
  }

  /** Updates the game timer display */
  private void updateGameTimer() {
    if (lblTimer != null && gameStartTime > 0) {
      long elapsed = (System.currentTimeMillis() - gameStartTime) / 1000;
      lblTimer.setText(elapsed + "s");
    }
  }

  /** Ends the game with win/lose result */
  private void endGame(boolean won) {
    gameRunning = false;

    // Stop timers
    if (gameTimer != null) {
      gameTimer.stop();
    }
    if (targetSpawnTimer != null) {
      targetSpawnTimer.stop();
    }

    // Clear remaining targets
    for (Button target : activeTargets) {
      gamePlayArea.getChildren().remove(target);
    }
    activeTargets.clear();

    // Update UI
    gamePlayArea.setVisible(false);
    gameResults.setVisible(true);
    btnStartGame.setDisable(false);

    if (won) {
      lblGameResult.setText("🎉 Training Complete!");
      lblGameResult.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #059669;");
      lblFinalStats.setText("You successfully completed Cookie Monster's auto-accept training!");
      btnStartGame.setText("Training Completed");
      btnStartGame.setStyle("-fx-font-size: 16px; -fx-background-color: #22c55e; -fx-text-fill: white; -fx-pref-width: 150; -fx-pref-height: 40; -fx-border-radius: 8; -fx-background-radius: 8;");
      
      // Mark interactive element as completed
      if (gameStateContext != null && currentParticipant == Participant.DEFENDANT) {
        gameStateContext.markInteractableDone(getRoleKeyOf(currentParticipant));
        interactableUsed = true;
        updateAimTrainingGameVisibility();
        updateChatControlsState(); // Enable chat controls after completing the game
      }

      // Notify chat about completion
      notifyAimTrainingCompletion();
    } else {
      lblGameResult.setText("❌ Training Failed");
      lblGameResult.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
      lblFinalStats.setText("3 consecutive misses! Cookie Monster needs more training.");
      btnStartGame.setText("Retry Training");
      btnStartGame.setStyle("-fx-font-size: 16px; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-pref-width: 150; -fx-pref-height: 40; -fx-border-radius: 8; -fx-background-radius: 8;");
    }
  }

  /** Notifies the chat system about aim training completion */
  private void notifyAimTrainingCompletion() {
    if (gameStateContext != null
        && gameStateContext.getChatController() != null
        && currentParticipant == Participant.DEFENDANT) {
      String interactionMessage =
          "You experienced Cookie Monster's auto-accept training simulation. The rapid-fire cookie "
          + "acceptance training reveals how the AI was programmed to prioritize speed over user consent...";

      // Add a system message to indicate the interaction happened
      if (txtaChat != null) {
        String currentText = txtaChat.getText();
        String systemMessage = "[SYSTEM] " + interactionMessage + "\n\n";
        txtaChat.setText(currentText + systemMessage);
      }

      System.out.println("DEBUG: Player completed Cookie Monster aim training game");
    }
  }
}
