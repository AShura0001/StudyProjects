package nz.ac.auckland.se206.persona;

import java.util.*;

public class ConversationBus {
  private static final int MAX_PER_ROLE = 6;
  private static ConversationBus INSTANCE;

  public static synchronized ConversationBus getInstance() {
    if (INSTANCE == null) INSTANCE = new ConversationBus();
    return INSTANCE;
  }

  private final Map<String, Deque<String>> lastByRole = new HashMap<>();

  public synchronized void post(String roleKey, String text) {
    if (roleKey == null || text == null || text.isBlank()) return;
    Deque<String> q = lastByRole.computeIfAbsent(roleKey, k -> new ArrayDeque<>());
    q.addLast(text.strip());
    while (q.size() > MAX_PER_ROLE) q.removeFirst();
  }

  /** Returns a compact summary of last remarks for everyone except excludeRole. */
  public synchronized String summarizeForContext(String excludeRole) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Deque<String>> e : lastByRole.entrySet()) {
      String role = e.getKey();
      if (role.equals(excludeRole)) continue;
      if (e.getValue().isEmpty()) continue;
      sb.append(roleLabel(role)).append(": ");
      sb.append(joinSamples(e.getValue()));
      sb.append("\n");
    }
    return sb.toString().strip();
  }

  private static String roleLabel(String role) {
    switch (role) {
      case "ai_defendant":
        return "AI Defendant said";
      case "ai_witness":
        return "AI Witness reported";
      case "human_witness":
        return "Human Witness recalled";
      case "player":
        return "Player asked";
      default:
        return role;
    }
  }

  private static String joinSamples(Deque<String> q) {
    // Take last 2 for brevity
    List<String> list = new ArrayList<>(q);
    int n = list.size();
    List<String> tail = list.subList(Math.max(0, n - 2), n);
    return String.join(" | ", tail);
  }
}
