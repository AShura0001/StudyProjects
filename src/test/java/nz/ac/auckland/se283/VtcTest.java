package nz.ac.auckland.se283;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nz.ac.auckland.se283.tools.AiClient;
import nz.ac.auckland.se283.vtc.VoiceToCodeTranscriber;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class VtcTest {
  @Test
  public void transcribeInvalidFormat() {

    byte[] audioData = new byte[1024 * 2];
    String format = "mp4";
    AiClient aiMock = Mockito.mock(AiClient.class);

    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");
    VoiceToCodeTranscriber vtcTrascriber = new VoiceToCodeTranscriber(aiMock);

    String result = vtcTrascriber.voiceToText(audioData, format);
    assertEquals("Invalid audio format", result);
  }

  @Test
  public void transcribeWavFormat() {

    byte[] audioData = new byte[1024 * 2];
    String format = "wav";
    AiClient aiMock = Mockito.mock(AiClient.class);

    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");
    VoiceToCodeTranscriber vtcTrascriber = new VoiceToCodeTranscriber(aiMock);

    String result = vtcTrascriber.voiceToText(audioData, format);
    assertEquals("this, this and that", result);
  }

  @Test
  public void transcribeM4aFormat() {

    byte[] audioData = new byte[1024 * 2];
    String format = "m4a";
    AiClient aiMock = Mockito.mock(AiClient.class);

    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");
    VoiceToCodeTranscriber vtcTrascriber = new VoiceToCodeTranscriber(aiMock);

    String result = vtcTrascriber.voiceToText(audioData, format);
    assertEquals("this, this and that", result);
  }

  @Test
  public void transcribeFlacFormat() {

    byte[] audioData = new byte[1024 * 2];
    String format = "flac";
    AiClient aiMock = Mockito.mock(AiClient.class);

    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");
    VoiceToCodeTranscriber vtcTrascriber = new VoiceToCodeTranscriber(aiMock);

    String result = vtcTrascriber.voiceToText(audioData, format);
    assertEquals("Invalid audio format", result);
  }

  @Test
  public void transcribeReallySmallFile() {

    byte[] audioData = new byte[0];
    String format = "m4a";
    AiClient aiMock = Mockito.mock(AiClient.class);

    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");
    VoiceToCodeTranscriber vtcTrascriber = new VoiceToCodeTranscriber(aiMock);

    String result = vtcTrascriber.voiceToText(audioData, format);
    assertEquals("Too small to transcribe!", result);
  }

  @Test
  public void transcribeJustBelowSmallFile() {

    byte[] audioData = new byte[1024 * 2 - 1];
    String format = "m4a";
    AiClient aiMock = Mockito.mock(AiClient.class);

    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");
    VoiceToCodeTranscriber vtcTrascriber = new VoiceToCodeTranscriber(aiMock);

    String result = vtcTrascriber.voiceToText(audioData, format);
    assertEquals("Too small to transcribe!", result);
  }

  @Test
  public void transcribeMinimumValidSizeMp3File() {

    byte[] audioData = new byte[1024 * 2];
    String format = "mp3";
    AiClient aiMock = Mockito.mock(AiClient.class);

    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");
    VoiceToCodeTranscriber vtcTrascriber = new VoiceToCodeTranscriber(aiMock);

    String result = vtcTrascriber.voiceToText(audioData, format);
    assertEquals("this, this and that", result);
  }
}
