package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import nz.ac.auckland.se206.GameStateContext;
import nz.ac.auckland.se206.prompts.PromptEngineering;
import nz.ac.auckland.se206.speech.TextToSpeech;

/**
 * Controller for the chat interface. Handles participant switching, LLM requests, and UI updates
 * for the chat window.
 */
@SuppressWarnings("unused")
public class ChatController {

  /** Enum for chat participants. */
  public enum Participant {
    DEFENDANT,
    AI_WITNESS,
    HUMAN_WITNESS
  }

  @FXML private TextArea txtaChat;
  @FXML private TextField txtInput;
  @FXML private Button btnSend;
  @FXML private Label lblStatus;

  private boolean hasSelectedParticipant = false;

  /** Thread state for each participant. */
  private static class ThreadState {
    ChatCompletionRequest req;
    final List<ChatMessage> history = new ArrayList<>(); // includes system/user/assistant
    boolean openingSent = false;
  }

  private final Map<Participant, ThreadState> threads = new EnumMap<>(Participant.class);
  private Participant currentParticipant = Participant.DEFENDANT;

  // Reference to game context for tracking participant interactions
  private GameStateContext gameContext;

  private final ExecutorService ioExecutor =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "chat-io");
            t.setDaemon(true);
            return t;
          });

  /** Initializes the chat controller after FXML load. */
  @FXML
  public void initialize() {
    lblStatus.setText("Click on a participant to chat");
    try {
      setupThreads();
    } catch (ApiProxyException e) {
      System.err.println("Warning: Could not initialize API configuration: " + e.getMessage());
      lblStatus.setText("API configuration not available - limited functionality");
    }
    replayCurrentHistory();

    // Get reference to game context for participant tracking
    gameContext = App.getGameStateContext();
  }

  /** Initializes threads for all participants. */
  private void setupThreads() throws ApiProxyException {
    for (Participant p : Participant.values()) {
      ensureThreadInitialized(p);
    }
  }

  /** Ensures a thread is initialized for the given participant. */
  private void ensureThreadInitialized(Participant p) throws ApiProxyException {
    if (threads.containsKey(p)) {
      return;
    }
    ApiProxyConfig config = ApiProxyConfig.readConfig();
    ChatCompletionRequest req =
        new ChatCompletionRequest(config)
            .setN(1)
            .setTemperature(0.3)
            .setTopP(0.8)
            .setModel(Model.GPT_4_1_MINI)
            .setMaxTokens(160);

    ThreadState state = new ThreadState();
    state.req = req;

    ChatMessage sys = new ChatMessage("system", getSystemPromptFor(p));
    state.history.add(sys);
    req.addMessage(sys);

    threads.put(p, state);
  }

  /** Loads the system prompt for the given participant. */
  private String getSystemPromptFor(Participant p) {
    Map<String, String> map = new java.util.HashMap<>();
    String promptId;
    switch (p) {
      case DEFENDANT:
        promptId = "defendant.txt";
        break;
      case AI_WITNESS:
        promptId = "ai_witness.txt";
        break;
      case HUMAN_WITNESS:
        promptId = "human_witness.txt";
        break;
      default:
        promptId = "chat.txt";
        break;
    }
    System.out.println("Loading prompt for " + p + " -> id=" + promptId);
    String prompt = PromptEngineering.getPrompt(promptId, map);
    if (!prompt.toLowerCase().contains("you are")) {
      System.out.println("Warning: suspect system prompt content for " + p);
    }
    return prompt;
  }

  /** Returns the display name for a participant. */
  private String displayName(Participant participant) {
    switch (participant) {
      case DEFENDANT:
        return "Defendant";
      case AI_WITNESS:
        return "AI Witness";
      case HUMAN_WITNESS:
        return "Human Witness";
      default:
        return "Unknown";
    }
  }

  /** Replays the chat history for the current participant. */
  private void replayCurrentHistory() {
    txtaChat.clear();
    ThreadState state = threads.get(currentParticipant);
    if (state == null) {
      return;
    }
    for (ChatMessage msg : state.history) {
      if ("system".equalsIgnoreCase(msg.getRole())) {
        continue; // don’t show system rows
      }
      appendChatMessage(currentParticipant, msg);
    }
  }

  /** Appends a chat message to the chat area. */
  private void appendChatMessage(Participant p, ChatMessage msg) {
    String who;
    String role = msg.getRole();
    if ("user".equalsIgnoreCase(role)) {
      who = "You";
    } else if ("assistant".equalsIgnoreCase(role)) {
      who = displayName(p);
    } else {
      who = capitalize(role);
    }
    String line = who + ": " + msg.getContent() + "\n\n";
    if (Platform.isFxApplicationThread()) {
      txtaChat.appendText(line);
    } else {
      Platform.runLater(() -> txtaChat.appendText(line));
    }
  }

  /** Capitalizes the first letter of a string. */
  private String capitalize(String s) {
    if (s == null || s.isEmpty()) {
      return s;
    }
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  /** Appends a meta message to the chat area. */
  private void appendMeta(String text) {
    String line = "— " + text + "\n";
    if (Platform.isFxApplicationThread()) {
      txtaChat.appendText(line);
    } else {
      Platform.runLater(() -> txtaChat.appendText(line));
    }
  }

  /** Sends a user message to the LLM and handles the response. */
  private void sendToLlm(Participant p, ChatMessage userMsg) {
    ThreadState state = threads.get(p);
    if (state == null) {
      appendMeta("No thread for " + displayName(p));
      return;
    }
    // Record and send user message
    state.history.add(userMsg);
    state.req.addMessage(userMsg);
    appendChatMessage(p, userMsg); // ensure it shows instantly

    ioExecutor.submit(
        () -> {
          try {
            ChatCompletionResult result = state.req.execute();
            Choice choice = result.getChoices().iterator().next();
            ChatMessage assistant = choice.getChatMessage();
            // Record assistant reply and add to request history
            state.history.add(assistant);
            state.req.addMessage(assistant);
            appendChatMessage(p, assistant);
            TextToSpeech.speak(assistant.getContent());
          } catch (ApiProxyException ex) {
            ex.printStackTrace();
            appendMeta("Error contacting LLM.");
          }
        });
  }

  /** Handles the send button action from FXML. */
  @FXML
  private void onSendMessage(ActionEvent event) throws IOException {
    String message = txtInput.getText().trim();
    if (message.isEmpty()) {
      return;
    }
    txtInput.clear();

    // Mark participant as contacted when player sends a message
    if (gameContext != null) {
      gameContext.markParticipantContacted(currentParticipant);
    }

    sendToLlm(currentParticipant, new ChatMessage("user", message));
  }

  /** Handles the go back button action from FXML. */
  @FXML
  private void onGoBack(ActionEvent event) throws IOException {
    App.setRoot("room");
  }

  // Optional: expose visible history for current participant (excluding system)

  /** Returns the visible chat history for the current participant. */
  public List<ChatMessage> getCurrentHistory() {
    ThreadState state = threads.get(currentParticipant);
    if (state == null) {
      return List.of();
    }
    List<ChatMessage> visible = new ArrayList<>();
    for (ChatMessage m : state.history) {
      if (!"system".equalsIgnoreCase(m.getRole())) {
        visible.add(m);
      }
    }
    return visible;
  }

  /** Shuts down the IO executor. */
  public void shutdown() {
    ioExecutor.shutdownNow();
  }

  /** Selects a participant and updates the UI. */
  public void selectParticipant(Participant participant) {
    currentParticipant = participant;
    hasSelectedParticipant = true;
    lblStatus.setText(displayName(participant));
    replayCurrentHistory();
    focusInput();
  }

  /** Focuses the input field for better UX. */
  public void focusInput() {
    Platform.runLater(
        () -> {
          if (txtInput != null) {
            txtInput.requestFocus();
          }
        });
  }

  /** Selects a participant by string ID. */
  public void selectParticipant(String id) {
    if (id == null) {
      selectParticipant(Participant.DEFENDANT);
      return;
    }
    String v = id.toLowerCase().replace("-", "_").trim();
    switch (v) {
      case "defendant":
      case "cookiemonster":
      case "cookie_monster":
        selectParticipant(Participant.DEFENDANT);
        break;
      case "ai_witness":
        // Sentri-Guard is always mapped to AI_WITNESS, which uses ai_witness.txt prompt
        selectParticipant(Participant.AI_WITNESS);
        break;
      case "human_witness":
      case "casey":
        selectParticipant(Participant.HUMAN_WITNESS);
        break;
      default:
        selectParticipant(Participant.DEFENDANT);
    }
  }

  /** Locks the chat input and send button. */
  public void lock() {
    txtInput.setDisable(true);
    btnSend.setDisable(true);
  }

  /** Unlocks the chat input and send button. */
  public void unlock() {
    txtInput.setDisable(false);
    btnSend.setDisable(false);
  }

  /** Requests the opening message from the LLM for a participant. */
  public void requestOpeningMessageFromLlm(Participant participant) {
    ThreadState state = threads.get(participant);
    selectParticipant(participant);
    if (state == null || state.openingSent) {
      return;
    }
    ioExecutor.submit(
        () -> {
          try {
            ChatMessage userMsg =
                new ChatMessage("user", "Please introduce yourself and explain the situation.");
            state.history.add(userMsg);
            state.req.addMessage(userMsg);
            appendChatMessage(participant, userMsg);
            ChatCompletionResult result = state.req.execute();
            Choice choice = result.getChoices().iterator().next();
            ChatMessage assistant = choice.getChatMessage();
            state.history.add(assistant);
            state.req.addMessage(assistant);
            appendChatMessage(participant, assistant);
            TextToSpeech.speak(assistant.getContent());
            state.openingSent = true; // only set after success
          } catch (ApiProxyException ex) {
            ex.printStackTrace();
            appendMeta("Error contacting LLM for opening message.");
            if (state != null) {
              state.openingSent = false;
            }
          }
        });
  }
}
