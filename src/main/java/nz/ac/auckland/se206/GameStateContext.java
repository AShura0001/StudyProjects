package nz.ac.auckland.se206;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javafx.application.Platform;
import nz.ac.auckland.se206.controllers.ChatController;
import nz.ac.auckland.se206.controllers.ChatController.Participant;
import org.yaml.snakeyaml.Yaml;

/**
 * Context for managing the game state. This version removes the old Guessing state and focuses on
 * gameplay with chat and room interactions only.
 */
@SuppressWarnings("unused")
public class GameStateContext {
  public void focusChatForParticipant(ChatController.Participant p) {
    if (chatController == null) return;
    javafx.application.Platform.runLater(() -> chatController.selectParticipant(p));
  }

  // Randomized gameplay data (still useful if you keep professions/rectangles)
  private final String rectIdToGuess;
  private final String professionToGuess;
  private final Map<String, String> rectanglesToProfession;

  // UI/controller hooks (injected after FXML load)
  private ChatController chatController;
  private RoomUiFacade roomUi;

  // Optional status presenter
  private java.util.function.Consumer<String> statusSink;

  // Track which participants have been contacted
  private final Set<Participant> contactedParticipants = new HashSet<>();

  public GameStateContext() {
    // Load professions.yaml (optional; keep if you still randomize labels)
    Map<String, Object> obj = null;
    Yaml yaml = new Yaml();
    try (InputStream inputStream =
        GameStateContext.class.getClassLoader().getResourceAsStream("data/professions.yaml")) {
      if (inputStream == null) {
        throw new IllegalStateException("File not found: data/professions.yaml");
      }
      obj = yaml.load(inputStream);
    } catch (IOException e) {
      e.printStackTrace();
    }

    @SuppressWarnings("unchecked")
    List<String> professions =
        obj != null
            ? (List<String>) obj.get("professions")
            : List.of("Defendant", "AI Witness", "Human Witness");

    Random random = new Random();
    Set<String> randomProfessions = new HashSet<>();
    while (randomProfessions.size() < 3 && !professions.isEmpty()) {
      String profession = professions.get(random.nextInt(professions.size()));
      randomProfessions.add(profession);
    }

    String[] randomProfessionsArray = randomProfessions.toArray(new String[3]);
    rectanglesToProfession = new HashMap<>();
    rectanglesToProfession.put("rectPerson1", safeArray(randomProfessionsArray, 0, "Defendant"));
    rectanglesToProfession.put("rectPerson2", safeArray(randomProfessionsArray, 1, "AI Witness"));
    rectanglesToProfession.put(
        "rectPerson3", safeArray(randomProfessionsArray, 2, "Human Witness"));

    int randomNumber = random.nextInt(3);
    rectIdToGuess =
        randomNumber == 0 ? "rectPerson1" : (randomNumber == 1 ? "rectPerson2" : "rectPerson3");
    professionToGuess = rectanglesToProfession.get(rectIdToGuess);
  }

  // -------------------- Game data accessors (optional) --------------------

  public String getProfessionToGuess() {
    return professionToGuess;
  }

  public String getRectIdToGuess() {
    return rectIdToGuess;
  }

  public String getProfession(String rectangleId) {
    return rectanglesToProfession.get(rectangleId);
  }

  // -------------------- UI/controller integration --------------------

  public void setChatController(ChatController controller) {
    this.chatController = controller;
  }

  public void setRoomUi(RoomUiFacade roomUi) {
    this.roomUi = roomUi;
  }

  public void setStatusSink(java.util.function.Consumer<String> sink) {
    this.statusSink = sink;
  }

  public void showChatPane() {
    // If your UI hides the chat, unhide here (no-op by default)
  }

  public void focusChatFor(String rectangleId) {
    if (chatController == null) return;
    Participant p = mapRectangleToParticipant(rectangleId, rectanglesToProfession.get(rectangleId));
    Platform.runLater(() -> chatController.selectParticipant(p));
  }

  public void lockChat() {
    if (chatController == null) return;
    Platform.runLater(chatController::lock);
  }

  public void unlockChat() {
    if (chatController == null) return;
    Platform.runLater(chatController::unlock);
  }

  public void lockRoom() {
    if (roomUi == null) return;
    Platform.runLater(roomUi::lockInteractions);
  }

  public void unlockRoom() {
    if (roomUi == null) return;
    Platform.runLater(roomUi::unlockInteractions);
  }

  public void showStatus(String message) {
    if (statusSink != null) {
      Platform.runLater(() -> statusSink.accept(message));
    }
  }

  public void requestOpeningMessage(ChatController.Participant p) {
    if (chatController != null) {
      chatController.requestOpeningMessageFromLlm(p);
    }
  }

  // -------------------- Participant tracking methods --------------------

  /** Marks a participant as contacted (player has sent at least one message). */
  public void markParticipantContacted(Participant participant) {
    contactedParticipants.add(participant);
  }

  /** Returns true if all three participants have been contacted. */
  public boolean hasContactedAllParticipants() {
    return contactedParticipants.size() >= 3;
  }

  /** Returns the set of participants that haven't been contacted yet. */
  public Set<Participant> getUncontactedParticipants() {
    Set<Participant> uncontacted = new HashSet<>();
    for (Participant p : Participant.values()) {
      if (!contactedParticipants.contains(p)) {
        uncontacted.add(p);
      }
    }
    return uncontacted;
  }

  /** Returns the number of participants contacted. */
  public int getContactedCount() {
    return contactedParticipants.size();
  }

  // -------------------- Helpers --------------------

  private static String safeArray(String[] arr, int idx, String fallback) {
    if (arr == null || idx < 0 || idx >= arr.length) return fallback;
    String v = arr[idx];
    return (v == null || v.isBlank()) ? fallback : v;
  }

  private Participant mapRectangleToParticipant(String rectangleId, String professionLabel) {
    // Explicit mapping by rectangle id
    if ("rectPerson1".equals(rectangleId)) return Participant.DEFENDANT;
    if ("rectPerson2".equals(rectangleId)) return Participant.AI_WITNESS;
    if ("rectPerson3".equals(rectangleId)) return Participant.HUMAN_WITNESS;

    // Fallback using label
    if (professionLabel != null) {
      String p = professionLabel.toLowerCase();
      if (p.contains("defendant")) return Participant.DEFENDANT;
      if (p.contains("ai") && p.contains("witness")) return Participant.AI_WITNESS;
      if (p.contains("human") && p.contains("witness")) return Participant.HUMAN_WITNESS;
    }
    return Participant.DEFENDANT;
  }

  // -------------------- Minimal facade for room interactions --------------------

  public interface RoomUiFacade {
    void lockInteractions();

    void unlockInteractions();
  }
}
