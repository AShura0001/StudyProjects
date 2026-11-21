package nz.ac.auckland.se283.vtc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import nz.ac.auckland.se283.tools.AiClient;
import nz.ac.auckland.se283.tools.ApiCaller;
import nz.ac.auckland.se283.tools.OpeniAiClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ApiCallingIntegrationTest {

  @Test
  public void transcribe_validDataSizeAndFormat_correctCode() throws IOException {
    // Arrange
    byte[] audioData = new byte[1024 * 3];
    String format = "mp3";

    ApiCaller caller = Mockito.mock(ApiCaller.class);
    AiClient client = new OpeniAiClient(caller);
    VoiceToCodeTranscriber transcriber = new VoiceToCodeTranscriber(client);

    Mockito.when(caller.call(audioData, format)).thenReturn(("this, this and that"));
    Mockito.when(
            caller.call(
                "Convert the following text to code based on the language format provided: "
                    + "this, this and that",
                "English"))
        .thenReturn(("while true: print(\"Hellow World!\")"));

    // Act
    String transcription = transcriber.voiceToText(audioData, format);
    String result = transcriber.textToCode(transcription);

    // Assert
    assertEquals("while true: print(\"Hellow World!\")", result);

    // Cleanup
    File file =
        new File(
            "src\\main\\java\\n"
                + //
                "z\\ac\\auckland\\se283\\generated\\GeneratedCode.txt");
    file.delete();
  }
}
