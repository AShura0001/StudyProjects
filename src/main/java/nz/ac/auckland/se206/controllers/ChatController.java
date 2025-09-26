package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import nz.ac.auckland.se206.persona.ConversationBus;
import nz.ac.auckland.se206.prompts.PromptEngineering;

/** Chat UI controller: participant threads, LLM calls, awareness integration, flashback sync. */
@SuppressWarnings("unused")
public class ChatController {

  /** Chat participants. */
  public enum Participant {
    DEFENDANT,
    AI_WITNESS,
    HUMAN_WITNESS
  }

  /** Optional flashback summaries posted once when first viewed. */
  private static final Map<String, String> FLASHBACK_SUMMARY =
      Map.of(
          "ai_defendant",
          "An uncloseable cookie pop-up blocks access until 'Accept All' is clicked, bypassing"
              + " explicit consent and triggering targeted ads.",
          "human_witness",
          "",
          "ai_witness",
          "");

  // Main chat UI
  @FXML private TextArea txtaChat;
  @FXML private TextField txtInput;
  @FXML private Button btnSend;
  @FXML private Label lblStatus;

  private boolean hasSelectedParticipant = false;

  /** Per-participant thread state. */
  private static class ThreadState {
    ChatCompletionRequest req;
    final List<ChatMessage> history = new ArrayList<>(); // includes system/user/assistant
    boolean openingSent = false;
  }

  private final Map<Participant, ThreadState> threads = new EnumMap<>(Participant.class);
  private Participant currentParticipant = Participant.DEFENDANT;

  // Game/persona context
  private GameStateContext gameContext;
  private String personaScenarioBrief = "";
  private Set<String> personaFlashbacksSeen = Collections.emptySet();
  private Map<String, Boolean> personaInteractableDone = Collections.emptyMap();

  // Flashback chat widgets (mirrors chat content & delegates send)
  private Label flashbackLblStatus;
  private TextArea flashbackTxtaChat;
  private TextField flashbackTxtInput;
  private Button flashbackBtnSend;

  private final ExecutorService ioExecutor =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "chat-io");
            t.setDaemon(true);
            return t;
          });

  // ---- Lifecycle ----

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
    gameContext = App.getGameStateContext();

    // Send on Enter
    if (txtInput != null) {
      txtInput.setOnKeyPressed(
          event -> {
            if (event.getCode().toString().equals("ENTER")) {
              if (!txtInput.isDisabled() && !btnSend.isDisabled()) {
                try {
                  onSendMessage(null);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
              event.consume();
            }
          });
    }
  }

  /** Allow Room/Persona to inject dynamic context (scenario, flashbacks, interactables). */
  public void setPersonaContext(
      String scenarioBrief, Set<String> flashbacksSeen, Map<String, Boolean> interactableDone) {
    this.personaScenarioBrief = (scenarioBrief == null) ? "" : scenarioBrief;
    this.personaFlashbacksSeen = (flashbacksSeen == null) ? Collections.emptySet() : flashbacksSeen;
    this.personaInteractableDone =
        (interactableDone == null) ? Collections.emptyMap() : interactableDone;
  }

  // ---- Thread setup & prompts ----

  private void setupThreads() throws ApiProxyException {
    for (Participant p : Participant.values()) {
      ensureThreadInitialized(p);
    }
  }

  private void ensureThreadInitialized(Participant p) throws ApiProxyException {
    if (threads.containsKey(p)) return;

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

  private static String roleKeyOf(Participant p) {
    switch (p) {
      case HUMAN_WITNESS:
        return "human_witness";
      case AI_WITNESS:
        return "ai_witness";
      default:
        return "ai_defendant";
    }
  }

  private String scenarioBrief() {
    if (personaScenarioBrief != null && !personaScenarioBrief.isBlank()) {
      return personaScenarioBrief;
    }
    try {
      if (gameContext != null && gameContext.getScenarioBrief() != null) {
        return gameContext.getScenarioBrief();
      }
    } catch (Throwable ignored) {
    }
    return "";
  }

  private Set<String> flashbacksSeen() {
    if (personaFlashbacksSeen != null && !personaFlashbacksSeen.isEmpty()) {
      return personaFlashbacksSeen;
    }
    try {
      if (gameContext != null && gameContext.getFlashbacksSeen() != null) {
        return gameContext.getFlashbacksSeen();
      }
    } catch (Throwable ignored) {
    }
    return Collections.emptySet();
  }

  private Map<String, Boolean> interactableDone() {
    if (personaInteractableDone != null && !personaInteractableDone.isEmpty()) {
      return personaInteractableDone;
    }
    try {
      if (gameContext != null && gameContext.getInteractableDone() != null) {
        return gameContext.getInteractableDone();
      }
    } catch (Throwable ignored) {
    }
    return Collections.emptyMap();
  }

  /** Dynamic system prompt built from persona/game context. */
  private String getSystemPromptFor(Participant p) {
    return PromptEngineering.systemForRole(
        roleKeyOf(p), scenarioBrief(), flashbacksSeen(), interactableDone());
  }

  private ChatMessage dynamicSystemFor(Participant p) {
    return new ChatMessage("system", getSystemPromptFor(p));
  }

  // ---- UI helpers ----

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

  private void replayCurrentHistory() {
    txtaChat.clear();
    ThreadState state = threads.get(currentParticipant);
    if (state == null) {
      scrollToBottom(txtaChat);
      return;
    }
    for (ChatMessage msg : state.history) {
      if ("system".equalsIgnoreCase(msg.getRole())) continue; // hide system rows
      appendChatMessage(currentParticipant, msg);
    }
    // ensure we land at the end even if no messages got appended above
    scrollToBottom(txtaChat);
  }

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
      scrollToBottom(txtaChat);
      // Mirror into flashback chat if present
      if (flashbackTxtaChat != null) {
        flashbackTxtaChat.appendText(line);
        scrollToBottom(flashbackTxtaChat);
      }
    } else {
      Platform.runLater(
          () -> {
            txtaChat.appendText(line);
            scrollToBottom(txtaChat);
            if (flashbackTxtaChat != null) {
              flashbackTxtaChat.appendText(line);
              scrollToBottom(flashbackTxtaChat);
            }
          });
    }
  }

  private String capitalize(String s) {
    if (s == null || s.isEmpty()) return s;
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  private void appendMeta(String text) {
    String line = "— " + text + "\n";
    if (Platform.isFxApplicationThread()) {
      txtaChat.appendText(line);
      scrollToBottom(txtaChat);
    } else {
      Platform.runLater(
          () -> {
            txtaChat.appendText(line);
            scrollToBottom(txtaChat);
          });
    }
  }

  /** Always jump TextArea caret to the end (works as scroll-to-bottom in JavaFX). */
  private void scrollToBottom(TextArea area) {
    if (area == null) return;
    Platform.runLater(
        () -> {
          int len = area.getText() == null ? 0 : area.getText().length();
          area.positionCaret(len);
          area.selectPositionCaret(len); // nudges viewport
          area.deselect(); // avoid selecting text
        });
  }

  // ---- LLM calls ----

  private void sendToLlm(Participant p, ChatMessage userMsg) {
    ThreadState state = threads.get(p);
    if (state == null) {
      appendMeta("No thread for " + displayName(p));
      return;
    }

    // IF cookies are not accepted, blocks ability to send messages to Cookie Monster
    if (p == Participant.DEFENDANT && gameContext != null && !gameContext.isCookiesAccepted()) {
      appendMeta("Accept cookies to chat with Cookie Monster!");
      return;
    }

    setLoadingState();

    // Awareness bus (player speaks)
    ConversationBus.getInstance().post("player", userMsg.getContent());

    ioExecutor.submit(
        () -> {
          try {
            // Build a fresh request with dynamic system + recent visible history
            ApiProxyConfig config = ApiProxyConfig.readConfig();
            ChatCompletionRequest fresh =
                new ChatCompletionRequest(config)
                    .setN(1)
                    .setTemperature(0.3)
                    .setTopP(0.8)
                    .setModel(Model.GPT_4_1_MINI)
                    .setMaxTokens(160);

            ChatMessage sysMsg = new ChatMessage("system", getSystemPromptFor(p));
            fresh.addMessage(sysMsg);

            // Last ~8 visible (non-system) messages for context
            List<ChatMessage> visible = new ArrayList<>();
            synchronized (state.history) {
              for (ChatMessage m : state.history) {
                if (!"system".equalsIgnoreCase(m.getRole())) visible.add(m);
              }
            }
            int start = Math.max(0, visible.size() - 8);
            for (int i = start; i < visible.size(); i++) {
              fresh.addMessage(visible.get(i));
            }

            fresh.addMessage(userMsg);

            // Echo locally (chat + state)
            Platform.runLater(() -> appendChatMessage(p, userMsg));
            synchronized (state.history) {
              state.history.add(userMsg);
            }

            // Call LLM
            ChatCompletionResult result = fresh.execute();
            Choice choice = result.getChoices().iterator().next();
            ChatMessage assistant = choice.getChatMessage();

            // Save reply + show
            synchronized (state.history) {
              state.history.add(assistant);
            }
            Platform.runLater(() -> appendChatMessage(p, assistant));

            // Awareness bus (role speaks)
            ConversationBus.getInstance().post(roleKeyOf(p), assistant.getContent());

            // Update thread's last request pointer
            state.req = fresh;

            clearLoadingState();
          } catch (Exception ex) {
            ex.printStackTrace();
            appendMeta("Error contacting LLM.");
            clearLoadingState();
          }
        });
  }

  // ---- FXML handlers ----

  @FXML
  private void onSendMessage(ActionEvent event) throws IOException {
    String message = txtInput.getText().trim();
    if (message.isEmpty()) return;
    txtInput.clear();

    // Mark participant as contacted
    if (gameContext != null) {
      gameContext.markParticipantContacted(currentParticipant);
    }

    sendToLlm(currentParticipant, new ChatMessage("user", message));
  }

  @FXML
  private void onGoBack(ActionEvent event) throws IOException {
    App.setRoot("room");
  }

  // ---- Flashback chat wiring ----

  /** Hook up flashback chat widgets so they mirror main chat and can send messages. */
  public void setFlashbackChatComponents(
      Label lblStatus, TextArea txtaChat, TextField txtInput, Button btnSend) {
    this.flashbackLblStatus = lblStatus;
    this.flashbackTxtaChat = txtaChat;
    this.flashbackTxtInput = txtInput;
    this.flashbackBtnSend = btnSend;
    updateFlashbackComponents();

    if (this.flashbackTxtInput != null) {
      this.flashbackTxtInput.setOnKeyPressed(
          event -> {
            if (event.getCode().toString().equals("ENTER")) {
              if (!this.flashbackTxtInput.isDisabled() && !this.flashbackBtnSend.isDisabled()) {
                onSendMessage();
              }
              event.consume();
            }
          });
    }
  }

  /** Keep flashback header/text in sync with current participant and chat area. */
  private void updateFlashbackComponents() {
    if (flashbackLblStatus != null) {
      flashbackLblStatus.setText(displayName(currentParticipant));
    }
    if (flashbackTxtaChat != null && txtaChat != null) {
      flashbackTxtaChat.setText(txtaChat.getText());
      scrollToBottom(flashbackTxtaChat);
    }
  }

  /** Flashback-pane send handler (delegates to same LLM pipeline). */
  public void onSendMessage() {
    if (flashbackTxtInput != null) {
      String message = flashbackTxtInput.getText().trim();
      if (message.isEmpty()) return;
      flashbackTxtInput.clear();

      if (gameContext != null) {
        gameContext.markParticipantContacted(currentParticipant);
      }

      sendToLlm(currentParticipant, new ChatMessage("user", message));
    }
  }

  // ---- Public helpers ----

  /** Visible (non-system) chat history for the current participant. */
  public List<ChatMessage> getCurrentHistory() {
    ThreadState state = threads.get(currentParticipant);
    if (state == null) return List.of();
    List<ChatMessage> visible = new ArrayList<>();
    synchronized (state.history) {
      for (ChatMessage m : state.history) {
        if (!"system".equalsIgnoreCase(m.getRole())) visible.add(m);
      }
    }
    return visible;
  }

  public void shutdown() {
    ioExecutor.shutdownNow();
  }

  /** Select a participant via enum and refresh UI. */
  public void selectParticipant(Participant participant) {
    currentParticipant = participant;
    hasSelectedParticipant = true;
    lblStatus.setText(displayName(participant));
    replayCurrentHistory(); // auto-scroll runs inside
    updateFlashbackComponents(); // keep flashback header in sync
    focusInput();
  }

  /** Select a participant via string id (aliases supported). */
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

  public void lock() {
    txtInput.setDisable(true);
    btnSend.setDisable(true);
  }

  public void unlock() {
    txtInput.setDisable(false);
    btnSend.setDisable(false);
  }

  /** Set UI to loading state while waiting for LLM response */
  private void setLoadingState() {
    Platform.runLater(
        () -> {
          btnSend.setDisable(true);
          txtInput.setDisable(true);
          txtInput.setPromptText("Waiting for response...");
          if (flashbackBtnSend != null) {
            flashbackBtnSend.setDisable(true);
          }
          if (flashbackTxtInput != null) {
            flashbackTxtInput.setDisable(true);
            flashbackTxtInput.setPromptText("Waiting for response...");
          }
        });
  }

  /** Reset UI from loading state */
  private void clearLoadingState() {
    Platform.runLater(
        () -> {
          btnSend.setDisable(false);
          txtInput.setDisable(false);
          txtInput.setPromptText("Type your message...");
          if (flashbackBtnSend != null) {
            flashbackBtnSend.setDisable(false);
          }
          if (flashbackTxtInput != null) {
            flashbackTxtInput.setDisable(false);
            flashbackTxtInput.setPromptText("Type your message...");
          }
        });
  }

  /** Ask the LLM to produce an opening line (injects a fresh dynamic system prompt). */
  public void requestOpeningMessageFromLlm(Participant participant) {
    ThreadState state = threads.get(participant);
    selectParticipant(participant);
    if (state == null || state.openingSent) return;

    // For Cookie Monster: require cookies accepted before sending opener
    if (participant == Participant.DEFENDANT
        && gameContext != null
        && !gameContext.isCookiesAccepted()) {
      return;
    }

    setLoadingState();

    ioExecutor.submit(
        () -> {
          try {
            ApiProxyConfig config = ApiProxyConfig.readConfig();
            ChatCompletionRequest fresh =
                new ChatCompletionRequest(config)
                    .setN(1)
                    .setTemperature(0.3)
                    .setTopP(0.8)
                    .setModel(Model.GPT_4_1_MINI)
                    .setMaxTokens(160);

            ChatMessage sysMsg = new ChatMessage("system", getSystemPromptFor(participant));
            fresh.addMessage(sysMsg);

            List<ChatMessage> visible = new ArrayList<>();
            synchronized (state.history) {
              for (ChatMessage m : state.history) {
                if (!"system".equalsIgnoreCase(m.getRole())) visible.add(m);
              }
            }
            int start = Math.max(0, visible.size() - 8);
            for (int i = start; i < visible.size(); i++) {
              fresh.addMessage(visible.get(i));
            }

            ChatMessage userMsg =
                new ChatMessage("user", "Please introduce yourself and explain the situation.");
            fresh.addMessage(userMsg);

            ConversationBus.getInstance().post("player", userMsg.getContent());

            ChatCompletionResult result = fresh.execute();
            Choice choice = result.getChoices().iterator().next();
            ChatMessage assistant = choice.getChatMessage();

            Platform.runLater(() -> appendChatMessage(participant, userMsg));
            Platform.runLater(() -> appendChatMessage(participant, assistant));

            synchronized (state.history) {
              state.history.add(userMsg);
              state.history.add(assistant);
            }
            ConversationBus.getInstance().post(roleKeyOf(participant), assistant.getContent());

            state.req = fresh;

            state.openingSent = true;
            clearLoadingState();
          } catch (ApiProxyException ex) {
            ex.printStackTrace();
            appendMeta("Error contacting LLM for opening message.");
            if (state != null) {
              state.openingSent = false;
            }
            clearLoadingState();
          }
        });
  }

  // ---- Flashback registration (call when popup/slideshow completes) ----

  public void onFlashbackCompleted(Participant p) {
    GameStateContext ctx = (gameContext != null) ? gameContext : App.getGameStateContext();
    if (ctx == null) return;

    String roleKey = roleKeyOf(p);
    Set<String> seen = ctx.getFlashbacksSeen();
    boolean firstTime = (seen == null) || !seen.contains(roleKey);

    ctx.markFlashbackSeen(p);

    if (firstTime) {
      String note = FLASHBACK_SUMMARY.getOrDefault(roleKey, "");
      if (!note.isBlank()) {
        ConversationBus.getInstance().post(roleKey, "[Flashback] " + note);
      }
    }
  }

  public void onFlashbackCompleted(String id) {
    if (id == null) return;
    String v = id.toLowerCase().replace("-", "_").trim();
    switch (v) {
      case "defendant":
      case "cookiemonster":
      case "cookie_monster":
        onFlashbackCompleted(Participant.DEFENDANT);
        break;
      case "ai_witness":
        onFlashbackCompleted(Participant.AI_WITNESS);
        break;
      case "human_witness":
      case "casey":
        onFlashbackCompleted(Participant.HUMAN_WITNESS);
        break;
      default:
        onFlashbackCompleted(Participant.DEFENDANT);
    }
  }

  /** Static helper (usable from FlashbackController) to record viewed flashback + post note. */
  public static void announceFlashbackViewedStatic(Participant p) {
    GameStateContext ctx = App.getGameStateContext();
    if (ctx == null) return;

    String roleKey = roleKeyOf(p);
    Set<String> seen = ctx.getFlashbacksSeen();
    boolean firstTime = (seen == null) || !seen.contains(roleKey);

    ctx.markFlashbackSeen(p);

    if (firstTime) {
      String note = FLASHBACK_SUMMARY.getOrDefault(roleKey, "");
      if (!note.isBlank()) {
        ConversationBus.getInstance().post(roleKey, "[Flashback] " + note);
      }
    }
  }

  public static void announceFlashbackViewedStatic(String id) {
    if (id == null) return;
    String v = id.toLowerCase().replace("-", "_").trim();
    switch (v) {
      case "defendant":
      case "cookiemonster":
      case "cookie_monster":
        announceFlashbackViewedStatic(Participant.DEFENDANT);
        break;
      case "ai_witness":
        announceFlashbackViewedStatic(Participant.AI_WITNESS);
        break;
      case "human_witness":
      case "casey":
        announceFlashbackViewedStatic(Participant.HUMAN_WITNESS);
        break;
      default:
        announceFlashbackViewedStatic(Participant.DEFENDANT);
    }
  }

  // ---- Focus & scroll helpers ----

  public void focusInput() {
    Platform.runLater(
        () -> {
          if (txtInput != null) {
            txtInput.requestFocus();
          }
          scrollToBottom(txtaChat);
          if (flashbackTxtaChat != null) {
            scrollToBottom(flashbackTxtaChat);
          }
        });
  }
}
