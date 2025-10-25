package nz.ac.auckland.se283.tools;

public class OpeniAiClient implements AiClient {
  private ApiCaller caller;

  public OpeniAiClient(ApiCaller caller) {
    this.caller = caller;
    caller.setModel("gpt-4.1-mini");
    caller.setN(1);
    caller.setTopP(1);
    caller.setTemperature(0.2);
    caller.setMaxTokens(10000);
  }

  @Override
  public String transcribe(byte[] audioData, String format) {
    caller.setMode("Audio-to-text");
    caller.setDataFormat(format);
    return caller.call(audioData, format);
  }

  @Override
  public String genCode(String text) {
    caller.setMode("Text-to-Code");
    String format = "English";
    String data =
        "Convert the following text to code based on the language format provided: " + text;
    return caller.call(data, format);
  }
}
