package nz.ac.auckland.se283.vtc;

import java.util.Set;
import nz.ac.auckland.se283.tools.AiClient;

public class VoiceToCodeTranscriber {
  private AiClient aiClient;
  private final Set<String> validFormats = Set.of("wav", "mp3", "flac", "m4a");

  public VoiceToCodeTranscriber(AiClient aiClient) {
    this.aiClient = aiClient;
  }

  public String transcribe(byte[] audioData, String format) {
    // check for valid format
    if (!isValidFormat(format)) {
      return "Invalid audio format";
    }
    return aiClient.transcribe(audioData, format);
  }

  private boolean isValidFormat(String format) {
    return validFormats.contains(format.toLowerCase());
  }
}
