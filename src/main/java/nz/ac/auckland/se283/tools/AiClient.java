package nz.ac.auckland.se283.tools;

public interface AiClient {
  String transcribe(byte[] audioData, String format);

  String genCode(String text);
}
