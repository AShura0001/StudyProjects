package nz.ac.auckland.se283;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nz.ac.auckland.se283.tools.AiClient;
import nz.ac.auckland.se283.vtc.VoiceToCodeTranscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class VtcTest {
  private AiClient aiMock;
  private VoiceToCodeTranscriber vtcTrascriber;

  @BeforeEach
  public void setUp() {
    this.aiMock = Mockito.mock(AiClient.class);
    this.vtcTrascriber = new VoiceToCodeTranscriber(aiMock);
  }

  @Test
  public void transcribe_whenFormatIsMp4_returnInvalidAudioFormat() {

    byte[] audioData = new byte[1024 * 2];
    String format = "mp4";
    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");

    String result = vtcTrascriber.voiceToText(audioData, format);

    assertEquals("Invalid audio format", result);
  }

  @Test
  public void transcribe_whenFormatIsWav_returnTranscription() {

    byte[] audioData = new byte[1024 * 2];
    String format = "wav";
    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");

    String result = vtcTrascriber.voiceToText(audioData, format);

    assertEquals("this, this and that", result);
  }

  @Test
  public void transcribe_whenFormatIsM4a_returnTranscription() {

    byte[] audioData = new byte[1024 * 2];
    String format = "m4a";
    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");

    String result = vtcTrascriber.voiceToText(audioData, format);

    assertEquals("this, this and that", result);
  }

  @Test
  public void transcribe_whenFormatIsFlac_returnInvalidAudioFormat() {

    byte[] audioData = new byte[1024 * 2];
    String format = "flac";
    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");

    String result = vtcTrascriber.voiceToText(audioData, format);

    assertEquals("Invalid audio format", result);
  }

  @Test
  public void transcribe_whenAudioDataIsEmpty_returnTooSmallToTranscribe() {

    byte[] audioData = new byte[0];
    String format = "m4a";
    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");

    String result = vtcTrascriber.voiceToText(audioData, format);

    assertEquals("Too small to transcribe!", result);
  }

  @Test
  public void transcribe_whenAudioDataIsJustBelowMinSize_returnTooSmall() {

    byte[] audioData = new byte[1024 * 2 - 1];
    String format = "m4a";
    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");

    String result = vtcTrascriber.voiceToText(audioData, format);

    assertEquals("Too small to transcribe!", result);
  }

  @Test
  public void transcribe_whenDataIsExactlyMinSize_returnTranscription() {

    byte[] audioData = new byte[1024 * 2];
    String format = "mp3";
    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");

    String result = vtcTrascriber.voiceToText(audioData, format);

    assertEquals("this, this and that", result);
  }

  @Test
  public void transcribe_whenDataIsJustAboveMaxSize_returnTooLarge() {

    byte[] audioData = new byte[(1024 * 1024 * 20) + 1];
    String format = "mp3";
    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");

    String result = vtcTrascriber.voiceToText(audioData, format);

    assertEquals("Too large to transcribe!", result);
  }

  @Test
  public void transcribe_whenDataIsExactlyMaxSize_returnTranscription() {

    byte[] audioData = new byte[(1024 * 1024 * 20)];
    String format = "mp3";
    Mockito.when(aiMock.transcribe(audioData, format)).thenReturn("this, this and that");

    String result = vtcTrascriber.voiceToText(audioData, format);

    assertEquals("this, this and that", result);
  }

  @Test
  public void textToCode_simpleInput_returnCode() {

    byte[] audioData = new byte[(1024 * 1024 * 20)];
    String format = "mp3";
    Mockito.when(aiMock.transcribe(audioData, format))
        .thenReturn("Move Straight. Turn right. Move straight.");

    String text = vtcTrascriber.voiceToText(audioData, format);
    Mockito.when(aiMock.genCode(text))
        .thenReturn(
            "List<Integer> movement = List.of(1,0,1)\r\n"
                + "List<Integer> direction = List.of(0,1,1)\\r\\n");

    String result = vtcTrascriber.textToCode(text);
    assertEquals(
        "List<Integer> movement = List.of(1,0,1);\r\nList<Integer> direction = List.of(0,1,1);\r\n",
        result);
  }
}
