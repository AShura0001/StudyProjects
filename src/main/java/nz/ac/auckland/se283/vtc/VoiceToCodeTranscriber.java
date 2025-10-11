package nz.ac.auckland.se283.vtc;

import nz.ac.auckland.se283.tools.AiClient;

public class VoiceToCodeTrascriber {
  private AiClient aiClient;

  public VoiceToCodeTrascriber(AiClient aiClient) {
    this.aiClient = aiClient;
  }

  public String transcribe(byte[] audioData, String format) {
    return aiClient.transcribe(audioData, format);
  }
}
