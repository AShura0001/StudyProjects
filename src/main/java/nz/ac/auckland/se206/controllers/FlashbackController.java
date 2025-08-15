package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest.Model;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.GameTimer;
import nz.ac.auckland.se206.prompts.PromptEngineering;

public class FlashbackController {

  @FXML private TextArea flashbackTxt;
  @FXML private TextField txtInput;
  @FXML private Button btnSend;
  @FXML private Button goBackBtn;
  @FXML private Label timerCounter;

  private ChatCompletionRequest chatCompletionRequest;
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
    return PromptEngineering.getPrompt("newchat.txt", map);
  }

  /**
   * Sets the participant for the chat context and initializes the ChatCompletionRequest.
   *
   * @param participant the participant to set
   */
  public void setParticipant(String participant) {
    this.participant = participant;
    try {
      ApiProxyConfig config = ApiProxyConfig.readConfig();
      chatCompletionRequest =
          new ChatCompletionRequest(config)
              .setN(1)
              .setTemperature(0.2)
              .setTopP(0.5)
              .setModel(Model.GPT_4_1_MINI)
              .setMaxTokens(100);
      runGpt(new ChatMessage("system", getSystemPrompt()));
    } catch (ApiProxyException e) {
      e.printStackTrace();
    }
  }

  /**
   * Appends a chat message to the chat text area.
   *
   * @param msg the chat message to append
   */
  private void appendChatMessage(ChatMessage msg) {
    flashbackTxt.appendText(msg.getRole() + ": " + msg.getContent() + "\n\n");
  }

  /**
   * Runs the GPT model with a given chat message.
   *
   * @param msg the chat message to process
   * @return the response chat message
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  private ChatMessage runGpt(ChatMessage msg) throws ApiProxyException {
    chatCompletionRequest.addMessage(msg);
    try {
      ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();
      Choice result = chatCompletionResult.getChoices().iterator().next();
      chatCompletionRequest.addMessage(result.getChatMessage());
      appendChatMessage(result.getChatMessage());
      // TextToSpeech.speak(result.getChatMessage().getContent());
      return result.getChatMessage();
    } catch (ApiProxyException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Runs the GPT model with a given chat message asynchronously.
   *
   * @param msg the chat message to process
   */
  private void runGptAsync(ChatMessage msg) {
    // Show loading indicator
    Platform.runLater(
        () -> {
          btnSend.setDisable(true);
          appendChatMessage(new ChatMessage("system", "Thinking..."));
        });

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
              // Update UI on JavaFX thread
              Platform.runLater(
                  () -> {
                    // Remove "Thinking..." message
                    String currentText = flashbackTxt.getText();
                    String withoutThinking = currentText.replace("system: Thinking...\n\n", "");
                    flashbackTxt.setText(withoutThinking);

                    // Add actual response
                    appendChatMessage(response);
                    btnSend.setDisable(false);
                  });
            })
        .exceptionally(
            throwable -> {
              Platform.runLater(
                  () -> {
                    btnSend.setDisable(false);
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

    // Call async version instead of blocking version
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
  private void onGoBack(ActionEvent event) throws ApiProxyException, IOException {
    cleanup(); // Unsubscribe before leaving
    App.setRoot("opening");
  }
}
