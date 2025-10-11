package nz.ac.auckland.se283.vtc;

import java.util.Set;
import nz.ac.auckland.se283.tools.AiClient;

public class VoiceToCodeTranscriber {
  private AiClient aiClient;
  private final Set<String> validFormats = Set.of("wav", "mp3", "m4a");

  public VoiceToCodeTranscriber(AiClient aiClient) {
    this.aiClient = aiClient;
  }

  public String voiceToText(byte[] audioData, String format) {
    // check for valid format
    if (isValidFormat(format)) {
      if (audioData == null || audioData.length <= (1024 * 2) - 1) {
        return "Too small to transcribe!";
      } else if (audioData.length >= (1024 * 1024 * 20) + 1) {
        return "Too large to transcribe!";
      }
      String transcription = aiClient.transcribe(audioData, format);
      return transcription;
    }
    return "Invalid audio format";
  }

  public String textToCode(String text) {
    return aiClient.genCode(text);
  }

  private boolean isValidFormat(String format) {
    return validFormats.contains(format.toLowerCase());
  }
}
