package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest.Model;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.ConversationManager;
import nz.ac.auckland.se206.GameTimer;
import nz.ac.auckland.se206.prompts.PromptEngineering;

public class FlashbackController {

  // UI elements for chat interface
  @FXML private TextArea flashbackTxt;
  @FXML private TextField txtInput;
  @FXML private Button btnSend;
  @FXML private Button goBackBtn;
  @FXML private Label timerCounter;

  // Evidence display elements (only for AI defendant)
  @FXML private Button evidenceBtn;
  @FXML private ImageView evidence1;
  @FXML private ImageView evidence2;
  @FXML private ImageView evidence3;

  // Track if evidence images are currently shown
  private boolean evidenceVisible = false;
  // GPT chat request handler
  private ChatCompletionRequest chatCompletionRequest;
  // Which participant we're talking to
  private String participant;

  /**
   * Initializes the chat view.
   *
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  @FXML
  public void initialize() throws ApiProxyException {
    // Any required initialization code can be placed here
    flashbackTxt.setEditable(false);
    flashbackTxt.setWrapText(true);

    // Subscribe to timer updates
    GameTimer.getInstance()
        .subscribe(
            "flashback",
            timeString -> {
              timerCounter.setText(timeString);
            });
  }

  /**
   * Generates the system prompt based on the profession.
   *
   * @return the system prompt string
   */
  private String getSystemPrompt() {
    Map<String, String> map = new HashMap<>();
    map.put("participant", participant);

    // Use the participant ID to get the right prompt
    return PromptEngineering.getPrompt("newchat.txt", map);
  }

  /**
   * Sets the participant for the chat context and initializes the ChatCompletionRequest.
   *
   * @param participant the participant to set
   */
  public void setParticipant(String participant) {
    this.participant = participant;

    // Show evidence button only for AI defendant
    if ("aiDefendantRect".equals(participant)) {
      evidenceBtn.setVisible(true);
    } else {
      evidenceBtn.setVisible(false);
    }

    // Always hide evidence images initially
    evidence1.setVisible(false);
    evidence2.setVisible(false);
    evidence3.setVisible(false);
    evidenceVisible = false;

    // Check if we have existing conversation
    boolean hasHistory = ConversationManager.getInstance().hasConversation(participant);

    if (hasHistory) {
      // Load existing conversation history if it exists!
      String history = ConversationManager.getInstance().getFormattedConversation(participant);
      flashbackTxt.setText(history);
      flashbackTxt.appendText("--- Conversation Resumed ---\n\n");
    } else {
      // Only show loading for NEW conversations
      Platform.runLater(
          () -> {
            flashbackTxt.setText("Loading conversation with " + participant + "...\n\n");
          });
    }

    // Load config and initialize chat asynchronously
    CompletableFuture.supplyAsync(
            () -> {
              try {
                return ApiProxyConfig.readConfig();
              } catch (ApiProxyException e) {
                throw new RuntimeException("Failed to load API config", e);
              }
            })
        .thenAcceptAsync(
            config -> {
              // Initialize chat completion request
              chatCompletionRequest =
                  new ChatCompletionRequest(config)
                      .setN(1)
                      .setTemperature(0.2)
                      .setTopP(0.5)
                      .setModel(Model.GPT_4_1_MINI)
                      .setMaxTokens(100);

              // Only send system message for NEW conversations
              if (!hasHistory) {
                ChatMessage systemMessage = new ChatMessage("system", getSystemPrompt());
                runGptAsync(systemMessage);
              } else {
                // Restore conversation to GPT context
                List<ChatMessage> history =
                    ConversationManager.getInstance().getConversation(participant);
                for (ChatMessage msg : history) {
                  chatCompletionRequest.addMessage(msg);
                }
              }
            })
        .exceptionally(
            throwable -> {
              Platform.runLater(
                  () -> {
                    flashbackTxt.setText("Error loading conversation: " + throwable.getMessage());
                  });
              throwable.printStackTrace();
              return null;
            });
  }

  /**
   * Appends a chat message to the chat text area.
   *
   * @param msg the chat message to append
   */
  private void appendChatMessage(ChatMessage msg) {
    ConversationManager.getInstance().addMessage(participant, msg);
    flashbackTxt.appendText(msg.getRole() + ": " + msg.getContent() + "\n\n");
  }

  /**
   * Runs the GPT model with a given chat message.
   *
   * @param msg the chat message to process
   * @return the response chat message
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */

  /**
   * Runs the GPT model with a given chat message asynchronously.
   *
   * @param msg the chat message to process
   */
  private void runGptAsync(ChatMessage msg) {
    // Don't show "Thinking..." for system messages
    if (!msg.getRole().equals("system")) {
      Platform.runLater(
          () -> {
            if (btnSend != null) btnSend.setDisable(true);
            appendChatMessage(new ChatMessage("system", "Thinking..."));
          });
    }

    CompletableFuture.supplyAsync(
            () -> {
              chatCompletionRequest.addMessage(msg);
              try {
                ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();
                Choice result = chatCompletionResult.getChoices().iterator().next();
                chatCompletionRequest.addMessage(result.getChatMessage());
                return result.getChatMessage();
              } catch (ApiProxyException e) {
                throw new RuntimeException(e);
              }
            })
        .thenAcceptAsync(
            response -> {
              Platform.runLater(
                  () -> {
                    // Remove "Thinking..." message if it was added
                    if (!msg.getRole().equals("system")) {
                      String currentText = flashbackTxt.getText();
                      String withoutThinking = currentText.replace("system: Thinking...\n\n", "");
                      flashbackTxt.setText(withoutThinking);
                    }

                    // Add actual response
                    appendChatMessage(response);
                    if (btnSend != null) btnSend.setDisable(false);
                  });

              // TTS disabled for flashback - don't uncomment this
              // CompletableFuture.runAsync(() -> TextToSpeech.speak(response.getContent()));

            })
        .exceptionally(
            throwable -> {
              Platform.runLater(
                  () -> {
                    if (btnSend != null) btnSend.setDisable(false);
                    appendChatMessage(
                        new ChatMessage("system", "Error: " + throwable.getMessage()));
                  });
              throwable.printStackTrace();
              return null;
            });
  }

  /**
   * Sends a message to the GPT model.
   *
   * @param event the action event triggered by the send button
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void onSendMessage(ActionEvent event) throws ApiProxyException, IOException {
    String message = txtInput.getText().trim();
    if (message.isEmpty()) {
      return;
    }
    txtInput.clear();
    ChatMessage msg = new ChatMessage("user", message);
    appendChatMessage(msg);

    // Doesn't block like runGpt()
    runGptAsync(msg);
  }

  // When navigating away from this scene
  public void cleanup() {
    GameTimer.getInstance().unsubscribe("flashback");
  }

  /**
   * Navigates back to the previous view.
   *
   * @param event the action event triggered by the go back button
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void onGoBack(ActionEvent event) throws IOException {
    GameTimer.getInstance().unsubscribe("flashback");
    App.setRoot("opening");
  }

  @FXML
  private void onEvidenceClick(ActionEvent event) {
    if (evidenceVisible) {
      // Hide evidence
      evidence1.setVisible(false);
      evidence2.setVisible(false);
      evidence3.setVisible(false);
      evidenceBtn.setText("Click to Look at the Crime Evidence Presented");
      evidenceVisible = false;
    } else {
      // Show evidence
      evidence1.setVisible(true);
      evidence2.setVisible(true);
      evidence3.setVisible(true);
      evidenceBtn.setText("Hide Evidence");
      evidenceVisible = true;
    }
  }
}
