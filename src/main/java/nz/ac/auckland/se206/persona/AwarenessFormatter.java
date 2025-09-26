package nz.ac.auckland.se206.persona;

import java.util.*;

/**
 * Formats the room "AWARENESS" string shown to all personas.
 *
 * <p>Supports: - Flashbacks viewed (by role) - Memories used / interactables completed (by role,
 * pretty labels) - Auto summary for AI Witness (Audit Log) when opened - Verbatim Audit Log bullets
 * (site — action — Allowed?) - Optional per-role Flashback Context text - Optional free-form Memory
 * Events
 *
 * <p>Controllers can push details with: AwarenessFormatter.addAuditItem("Youtube.com","Watched gym
 * recovery video","Yes"); AwarenessFormatter.setFlashbackContext("ai_defendant","Cookie Monster
 * auto-accepted..."); AwarenessFormatter.addMemoryEvent("ai_witness","Opened Simple Audit Log");
 */
public class AwarenessFormatter {

  // Thread-safe stores for extra awareness items
  private static final List<String> auditBullets = Collections.synchronizedList(new ArrayList<>());
  private static final Map<String, String> flashbackContext =
      Collections.synchronizedMap(new LinkedHashMap<>());
  private static final List<String> memoryEvents = Collections.synchronizedList(new ArrayList<>());

  /**
   * Build the awareness string from session state plus any registered extras. This method is
   * designed to be re-run for every LLM call so personas always see the freshest, QUOTABLE
   * awareness.
   */
  public static String fromSession(
      Set<String> flashbacksSeen, Map<String, Boolean> interactableDone) {

    StringBuilder sb = new StringBuilder();

    // 1) Flashbacks viewed
    if (flashbacksSeen != null && !flashbacksSeen.isEmpty()) {
      sb.append("Flashbacks viewed: ");
      sb.append(String.join(", ", sortedNiceRoles(flashbacksSeen)));
      sb.append(". ");
    }

    // 2) Memories used (interactables)
    if (interactableDone != null && !interactableDone.isEmpty()) {
      List<String> doneKeys = new ArrayList<>();
      for (Map.Entry<String, Boolean> e : interactableDone.entrySet()) {
        if (Boolean.TRUE.equals(e.getValue())) doneKeys.add(e.getKey());
      }
      if (!doneKeys.isEmpty()) {
        List<String> pretty = new ArrayList<>(doneKeys);
        pretty.replaceAll(AwarenessFormatter::prettyMemory);
        Collections.sort(pretty);
        sb.append("Memories used: ");
        sb.append(String.join(", ", pretty));
        sb.append(". ");
      }
    }

    // 2a) If AI Witness memory is opened, inject a concise, quotable summary
    //     so all personas know the key points even if no bullets were added yet.
    if (interactableDone != null && Boolean.TRUE.equals(interactableDone.get("ai_witness"))) {
      sb.append(
          "AI Witness memory (Audit Log) contents: some items were ALLOWED without consent by"
              + " Cookie Monster; some were BLOCKED by the human; example chain: watched a gym"
              + " recovery video → cross-site tracking → saw gym supplement ads (e.g., Youtube.com"
              + " — video view; Susweb.com — tracking, no consent; Goldsgym.com — targeted"
              + " supplement ad). ");
    }

    // 3) Optional memory events (short free-text lines)
    List<String> snapMem;
    synchronized (memoryEvents) {
      snapMem = new ArrayList<>(memoryEvents);
    }
    if (!snapMem.isEmpty()) {
      sb.append("\n[AWARENESS] Memory events:\n");
      for (String m : snapMem) {
        sb.append("- ").append(m).append("\n");
      }
    }

    // 4) Optional per-role flashback context (concise text per role, quotable)
    Map<String, String> snapCtx;
    synchronized (flashbackContext) {
      snapCtx = new LinkedHashMap<>(flashbackContext); // keep insertion order
    }
    if (!snapCtx.isEmpty()) {
      sb.append("\n[AWARENESS] Flashback context (by role):\n");
      for (Map.Entry<String, String> e : snapCtx.entrySet()) {
        sb.append("- ")
            .append(prettyRole(e.getKey()))
            .append(": ")
            .append(e.getValue())
            .append("\n");
      }
    }

    // 5) Verbatim audit bullets (site — action — Allowed?) for Sentri-Guard memory
    List<String> snapAudit;
    synchronized (auditBullets) {
      snapAudit = new ArrayList<>(auditBullets);
    }
    if (!snapAudit.isEmpty()) {
      sb.append("\n[AWARENESS] Simple Audit Log (Sentri-Guard / AI Witness):\n");
      for (String b : snapAudit) {
        // already preformatted, personas can quote these lines verbatim
        sb.append("- ").append(b).append("\n");
      }
    }

    return sb.toString().strip();
  }

  // ===== Controller-facing helpers =====

  /** Register one audit line visible to everyone (QUOTABLE). */
  public static void addAuditItem(String site, String action, String allowed) {
    if (site == null || action == null || allowed == null) return;
    String line = "\"" + site + " — " + action + " — Allowed? " + allowed + "\"";
    synchronized (auditBullets) {
      auditBullets.add(line);
    }
  }

  /** Clear all audit lines. */
  public static void clearAuditItems() {
    synchronized (auditBullets) {
      auditBullets.clear();
    }
  }

  /**
   * Set/replace concise flashback context text for a role: roleKey is one of "ai_defendant",
   * "ai_witness", "human_witness". Keep text brief and quotable.
   */
  public static void setFlashbackContext(String roleKey, String context) {
    if (roleKey == null || context == null) return;
    synchronized (flashbackContext) {
      flashbackContext.put(roleKey, context);
    }
  }

  /** Remove all flashback contexts. */
  public static void clearFlashbackContexts() {
    synchronized (flashbackContext) {
      flashbackContext.clear();
    }
  }

  /** Add a concise memory event line (e.g., "AI Witness: Opened Simple Audit Log"). */
  public static void addMemoryEvent(String ownerRoleKey, String event) {
    if (event == null) return;
    String role = (ownerRoleKey == null) ? "" : prettyRole(ownerRoleKey);
    String line = (role.isEmpty() ? "" : role + ": ") + event;
    synchronized (memoryEvents) {
      memoryEvents.add(line);
    }
  }

  /** Clear memory events. */
  public static void clearMemoryEvents() {
    synchronized (memoryEvents) {
      memoryEvents.clear();
    }
  }

  // ===== Pretty-print helpers =====

  private static List<String> sortedNiceRoles(Collection<String> roleKeys) {
    List<String> out = new ArrayList<>(roleKeys);
    out.replaceAll(AwarenessFormatter::prettyRole);
    Collections.sort(out);
    return out;
  }

  /** Map role keys to nice labels for flashbacks. */
  private static String prettyRole(String r) {
    if (r == null) return "(unknown)";
    switch (r) {
      case "ai_defendant":
        return "AI Defendant";
      case "ai_witness":
        return "AI Witness";
      case "human_witness":
        return "Human Witness";
      default:
        return r;
    }
  }

  /** Pretty names for memories/interactables. */
  private static String prettyMemory(String r) {
    if (r == null) return "(unknown memory)";
    switch (r) {
      case "ai_witness":
        return "AI Witness (Audit Log)";
      case "ai_defendant":
        return "AI Defendant (Cookie pop-up)";
      case "human_witness":
        return "Human Witness (Frustration gauge)";
      default:
        return r;
    }
  }
}
