package nz.ac.auckland.se206;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;

public class ConversationManager {
  // Saves the chat history in hash map for each unique participant
  private static final ConversationManager INSTANCE = new ConversationManager();
  private final ConcurrentHashMap<String, List<ChatMessage>> conversations =
      new ConcurrentHashMap<>();

  private ConversationManager() {}

  public static ConversationManager getInstance() {
    return INSTANCE;
  }

  /** Get conversation history for a participant */
  public List<ChatMessage> getConversation(String participant) {
    return conversations.computeIfAbsent(participant, k -> new ArrayList<>());
  }

  /** Add a message to participant's conversation */
  public void addMessage(String participant, ChatMessage message) {
    List<ChatMessage> conversation = getConversation(participant);
    conversation.add(message);
  }

  /** Check if participant has any conversation history */
  public boolean hasConversation(String participant) {
    List<ChatMessage> conversation = conversations.get(participant);
    return conversation != null && !conversation.isEmpty();
  }

  /** Clear conversation for a participant */
  public void clearConversation(String participant) {
    conversations.remove(participant);
  }

  /** Clear all conversations (e.g., when starting new game) */
  public void clearAll() {
    conversations.clear();
  }

  /** Get formatted conversation text for display */
  public String getFormattedConversation(String participant) {
    List<ChatMessage> conversation = getConversation(participant);
    StringBuilder formatted = new StringBuilder();

    for (ChatMessage msg : conversation) {
      formatted.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n\n");
    }

    return formatted.toString();
  }
}
