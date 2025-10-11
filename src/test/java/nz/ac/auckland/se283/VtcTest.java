package nz.ac.auckland.se283;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nz.ac.auckland.se283.tools.AiClient;
import nz.ac.auckland.se283.vtc.VoiceToCodeTrascriber;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class VtcTest {
  @Test
  public void transcribeInvalidFormat() {

    byte[] audioData = new byte[1024 * 2];
    String format = "mp4";
    AiClient aiMock = Mockito.mock(AiClient.class);

    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");
    VoiceToCodeTrascriber vtcTrascriber = new VoiceToCodeTrascriber(aiMock);

    String result = vtcTrascriber.transcribe(audioData, format);
    assertEquals("Invalid audio format", result);
  }
}
