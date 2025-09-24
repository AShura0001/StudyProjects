package nz.ac.auckland.se206.prompts;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import nz.ac.auckland.se206.persona.AwarenessFormatter;
import nz.ac.auckland.se206.persona.ConversationBus;
import nz.ac.auckland.se206.persona.PersonaProfile;

/** Prompt utilities: legacy template fill + dynamic persona/awareness system prompts. */
public class PromptEngineering {

  public static String getPrompt(String promptId, Map<String, String> data) {
    try {
      URL resourceUrl = PromptEngineering.class.getClassLoader().getResource("prompts/" + promptId);
      if (resourceUrl == null) {
        throw new IllegalArgumentException("Prompt file not found: " + promptId);
      }
      String template = loadTemplate(resourceUrl.toURI());
      return fillTemplate(template, data);
    } catch (IOException | URISyntaxException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Error loading or filling the prompt template.", e);
    }
  }

  private static String loadTemplate(URI filePath) throws IOException {
    return new String(Files.readAllBytes(Paths.get(filePath)));
  }

  private static String fillTemplate(String template, Map<String, String> data) {
    for (Map.Entry<String, String> entry : data.entrySet()) {
      template = template.replace("{" + entry.getKey() + "}", entry.getValue());
    }
    return template;
  }

  /** Persona + cross-awareness system prompt. */
  public static String systemForRole(
      String roleKey,
      String scenarioBrief,
      Set<String> flashbacksSeen,
      Map<String, Boolean> interactableDone) {
    PersonaProfile persona = PersonaProfile.fromRoleKey(roleKey);
    String awareness = AwarenessFormatter.fromSession(flashbacksSeen, interactableDone);
    String recent = ConversationBus.getInstance().summarizeForContext(roleKey);
    return persona.buildSystemPrompt(
        nullToEmpty(scenarioBrief), nullToEmpty(awareness), nullToEmpty(recent));
  }

  /** Overload if PersonaProfile is pre-resolved. */
  public static String systemForRole(
      PersonaProfile persona,
      String scenarioBrief,
      Set<String> flashbacksSeen,
      Map<String, Boolean> interactableDone) {
    String awareness = AwarenessFormatter.fromSession(flashbacksSeen, interactableDone);
    String recent = ConversationBus.getInstance().summarizeForContext(persona.roleKey);
    return persona.buildSystemPrompt(
        nullToEmpty(scenarioBrief), nullToEmpty(awareness), nullToEmpty(recent));
  }

  private static String nullToEmpty(String s) {
    return (s == null) ? "" : s;
  }
}
