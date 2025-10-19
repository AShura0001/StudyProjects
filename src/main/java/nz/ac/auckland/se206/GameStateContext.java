package nz.ac.auckland.se206;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javafx.application.Platform;
import nz.ac.auckland.se206.controllers.ChatController;
import nz.ac.auckland.se206.controllers.ChatController.Participant;
import nz.ac.auckland.se206.persona.AwarenessFormatter;
import nz.ac.auckland.se206.persona.ConversationBus;
import org.yaml.snakeyaml.Yaml;

/** Central game state: scenario brief, awareness, randomized data, and UI hooks. */
@SuppressWarnings("unused")
public class GameStateContext {

  // -------- Persona / Awareness --------
  private String scenarioBrief = "";
  private final Set<String> flashbacksSeen = new HashSet<>();
  private final Map<String, Boolean> interactableDone = new HashMap<>();

  // Track which owners’ flashbacks have already been broadcast
  private final Set<String> flashbackPrimedOwners = new HashSet<>();
  private final Set<Participant> slideshowSeen = new HashSet<>();

  public boolean hasSeenSlideshow(Participant p) {
    return slideshowSeen.contains(p);
  }

  public void markSlideshowSeen(Participant p) {
    slideshowSeen.add(p);
  }

  public String getScenarioBrief() {
    return scenarioBrief;
  }

  public void setScenarioBrief(String brief) {
    this.scenarioBrief = (brief == null) ? "" : brief;
  }

  /** Role keys seen: ai_defendant, ai_witness, human_witness */
  public Set<String> getFlashbacksSeen() {
    return Collections.unmodifiableSet(flashbacksSeen);
  }

  /** Interactable flags: roleKey -> done? */
  public Map<String, Boolean> getInteractableDone() {
    return Collections.unmodifiableMap(interactableDone);
  }

  public void markFlashbackSeen(String roleKey) {
    if (roleKey != null && !roleKey.isBlank()) {
      flashbacksSeen.add(roleKey);
    }
  }

  public void markFlashbackSeen(Participant p) {
    markFlashbackSeen(roleKeyOf(p));
  }

  public void markInteractableDone(String roleKey) {
    if (roleKey != null && !roleKey.isBlank()) {
      interactableDone.put(roleKey, true);
    }
  }

  public void markInteractableDone(Participant p) {
    markInteractableDone(roleKeyOf(p));
  }

  public void resetAwareness() {
    flashbacksSeen.clear();
    interactableDone.clear();
    flashbackPrimedOwners.clear();
  }

  public boolean isCookiesAccepted() {
    return Boolean.TRUE.equals(interactableDone.get("cookies_accepted"));
  }

  public void acceptCookies() {
    interactableDone.put("cookies_accepted", true);
  }

  private static String roleKeyOf(Participant p) {
    if (p == null) return "ai_defendant";
    switch (p) {
      case HUMAN_WITNESS:
        return "human_witness";
      case AI_WITNESS:
        return "ai_witness";
      default:
        return "ai_defendant";
    }
  }

  private static String flashbackOwnerLabel(Participant p) {
    if (p == null) return "Cookie Monster";
    switch (p) {
      case HUMAN_WITNESS:
        return "Casey";
      case AI_WITNESS:
        return "Sentri-Guard";
      case DEFENDANT:
      default:
        return "Cookie Monster";
    }
  }

  // -------- Flashback broadcasting --------

  /**
   * Broadcast this owner’s flashback context.
   *
   * <p>Owner gets a detailed private copy, while the whole room receives a concise awareness
   * summary that all personas may reference.
   */
  public synchronized boolean broadcastFlashbackIfNeeded(Participant owner, String conciseSummary) {
    Participant actualOwner = (owner == null) ? Participant.DEFENDANT : owner;
    String roleKey = roleKeyOf(actualOwner);
    if (roleKey == null || roleKey.isBlank()) return false;
    markFlashbackSeen(actualOwner);

    boolean alreadyPrimed = flashbackPrimedOwners.contains(roleKey);
    String summary = (conciseSummary == null) ? "" : conciseSummary.trim();

    if (!summary.isBlank()) {
      try {
        AwarenessFormatter.setFlashbackContext(roleKey, summary);
        if (!alreadyPrimed) {
          AwarenessFormatter.addMemoryEvent(
              "flashback", flashbackOwnerLabel(actualOwner) + " flashback: " + summary);
        }
      } catch (Throwable ignored) {
      }
    }

    if (alreadyPrimed) {
      return false;
    }

    // 1) Private context for the owner
    String privateKey = "flashback_private_" + roleKey;
    if (!summary.isBlank()) {
      String ownerMsg =
          "[FLASHBACK CONTEXT for "
              + actualOwner.name()
              + "]\n"
              + summary
              + "\nYou may rely on this memory when speaking as "
              + actualOwner.name()
              + ".";
      ConversationBus.getInstance().post(privateKey, ownerMsg);
    }

    // 2) Shared awareness for everyone
    String roomAwareness =
        "[AWARENESS] Flashback viewed ("
            + actualOwner.name()
            + "): "
            + (summary.isBlank() ? "(details pending)" : summary);
    ConversationBus.getInstance().post("room", roomAwareness);

    flashbackPrimedOwners.add(roleKey);
    return true;
  }

  // -------- Legacy randomized gameplay data --------
  private final String rectIdToGuess;
  private final String professionToGuess;
  private final Map<String, String> rectanglesToProfession;

  // UI/controller hooks
  private ChatController chatController;
  private RoomUiFacade roomUi;

  private java.util.function.Consumer<String> statusSink;
  private final Set<Participant> contactedParticipants = new HashSet<>();

  public GameStateContext() {
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

    int randomNumber = new Random().nextInt(3);
    rectIdToGuess =
        randomNumber == 0 ? "rectPerson1" : (randomNumber == 1 ? "rectPerson2" : "rectPerson3");
    professionToGuess = rectanglesToProfession.get(rectIdToGuess);
  }

  // -------- Game data accessors --------
  public String getProfessionToGuess() {
    return professionToGuess;
  }

  public String getRectIdToGuess() {
    return rectIdToGuess;
  }

  public String getProfession(String rectangleId) {
    return rectanglesToProfession.get(rectangleId);
  }

  // -------- UI/controller integration --------
  public void setChatController(ChatController controller) {
    this.chatController = controller;
  }

  public ChatController getChatController() {
    return this.chatController;
  }

  public void setRoomUi(RoomUiFacade roomUi) {
    this.roomUi = roomUi;
  }

  public void setStatusSink(java.util.function.Consumer<String> sink) {
    this.statusSink = sink;
  }

  public void showChatPane() {}

  public void focusChatForParticipant(ChatController.Participant p) {
    if (chatController == null) return;
    Platform.runLater(() -> chatController.selectParticipant(p));
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

  // -------- Participant tracking --------
  public void markParticipantContacted(Participant participant) {
    contactedParticipants.add(participant);
  }

  /** Checks if this is the first interaction with a participant. */
  public boolean isFirstInteraction(Participant participant) {
    return !contactedParticipants.contains(participant);
  }

  public boolean hasContactedAllParticipants() {
    return contactedParticipants.size() >= 3;
  }

  public Set<Participant> getUncontactedParticipants() {
    Set<Participant> uncontacted = new HashSet<>();
    for (Participant p : Participant.values()) {
      if (!contactedParticipants.contains(p)) {
        uncontacted.add(p);
      }
    }
    return uncontacted;
  }

  public int getContactedCount() {
    return contactedParticipants.size();
  }

  // -------- Helpers --------
  private static String safeArray(String[] arr, int idx, String fallback) {
    if (arr == null || idx < 0 || idx >= arr.length) return fallback;
    String v = arr[idx];
    return (v == null || v.isBlank()) ? fallback : v;
  }

  private Participant mapRectangleToParticipant(String rectangleId, String professionLabel) {
    if ("rectPerson1".equals(rectangleId)) return Participant.DEFENDANT;
    if ("rectPerson2".equals(rectangleId)) return Participant.AI_WITNESS;
    if ("rectPerson3".equals(rectangleId)) return Participant.HUMAN_WITNESS;

    if (professionLabel != null) {
      String p = professionLabel.toLowerCase();
      if (p.contains("defendant")) return Participant.DEFENDANT;
      if (p.contains("ai") && p.contains("witness")) return Participant.AI_WITNESS;
      if (p.contains("human") && p.contains("witness")) return Participant.HUMAN_WITNESS;
    }
    return Participant.DEFENDANT;
  }

  // -------- Room UI facade --------
  public interface RoomUiFacade {
    void lockInteractions();

    void unlockInteractions();
  }
}
