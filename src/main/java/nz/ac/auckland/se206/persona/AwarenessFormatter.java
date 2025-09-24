package nz.ac.auckland.se206.persona;

import java.util.*;

// Turn session flags into a compact paragraph shared with personas.
public class AwarenessFormatter {
  public static String fromSession(
      Set<String> flashbacksSeen, Map<String, Boolean> interactableDone) {
    StringBuilder sb = new StringBuilder();
    if (flashbacksSeen != null && !flashbacksSeen.isEmpty()) {
      sb.append("Flashbacks viewed: ");
      sb.append(String.join(", ", sortedNice(flashbacksSeen)));
      sb.append(". ");
    }
    if (interactableDone != null && !interactableDone.isEmpty()) {
      List<String> done = new ArrayList<>();
      for (Map.Entry<String, Boolean> e : interactableDone.entrySet()) {
        if (Boolean.TRUE.equals(e.getValue())) done.add(e.getKey());
      }
      if (!done.isEmpty()) {
        sb.append("Interactables completed: ");
        sb.append(String.join(", ", sortedNice(done)));
        sb.append(". ");
      }
    }
    return sb.toString().strip();
  }

  private static List<String> sortedNice(Collection<String> c) {
    List<String> out = new ArrayList<>(c);
    out.replaceAll(AwarenessFormatter::prettyRole);
    Collections.sort(out);
    return out;
  }

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
}
