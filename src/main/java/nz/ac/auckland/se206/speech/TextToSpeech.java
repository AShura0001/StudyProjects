package nz.ac.auckland.se206.speech;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.concurrent.Task;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.apiproxy.tts.TextToSpeechRequest;
import nz.ac.auckland.apiproxy.tts.TextToSpeechRequest.Provider;
import nz.ac.auckland.apiproxy.tts.TextToSpeechRequest.Voice;
import nz.ac.auckland.apiproxy.tts.TextToSpeechResult;

/** A utility class for converting text to speech using the specified API proxy. */
public class TextToSpeech {

  // Cache mapping short phrase -> generated audio URL to avoid repeated network latency.
  private static final Map<String, String> audioCache = new ConcurrentHashMap<>();

  /**
   * Converts the given text to speech and plays the audio.
   *
   * @param text the text to be converted to speech
   * @throws IllegalArgumentException if the text is null or empty
   */
  public static void speak(String text) {
    if (text == null || text.isEmpty()) {
      throw new IllegalArgumentException("Text should not be null or empty");
    }

    Task<Void> backgroundTask =
        new Task<>() {
          @Override
          protected Void call() {
            try {
              ApiProxyConfig config = ApiProxyConfig.readConfig();
              Provider provider = Provider.GOOGLE;
              Voice voice = Voice.GOOGLE_EN_US_STANDARD_H;

              TextToSpeechRequest ttsRequest = new TextToSpeechRequest(config);
              ttsRequest.setText(text).setProvider(provider).setVoice(voice);

              TextToSpeechResult ttsResult = ttsRequest.execute();
              String audioUrl = ttsResult.getAudioUrl();

              try (InputStream inputStream =
                  new BufferedInputStream(URI.create(audioUrl).toURL().openStream())) {
                Player player = new Player(inputStream);
                player.play();
              } catch (JavaLayerException | IOException e) {
                e.printStackTrace();
              }

            } catch (ApiProxyException e) {
              e.printStackTrace();
            }
            return null;
          }
        };

    Thread backgroundThread = new Thread(backgroundTask);
    backgroundThread.setDaemon(true); // Ensure the thread does not prevent JVM shutdown
    backgroundThread.start();
  }

  /**
   * Pre-generate and cache the audio URL for a short phrase so subsequent play is faster. This
   * performs the network request on a background thread and stores the resulting URL.
   */
  public static void preload(String text) {
    if (text == null || text.isEmpty()) return;
    if (audioCache.containsKey(text)) return;

    Task<Void> preloadTask =
        new Task<>() {
          @Override
          protected Void call() {
            try {
              ApiProxyConfig config = ApiProxyConfig.readConfig();
              Provider provider = Provider.GOOGLE;
              Voice voice = Voice.GOOGLE_EN_US_STANDARD_H;

              TextToSpeechRequest ttsRequest = new TextToSpeechRequest(config);
              ttsRequest.setText(text).setProvider(provider).setVoice(voice);
              TextToSpeechResult ttsResult = ttsRequest.execute();
              audioCache.put(text, ttsResult.getAudioUrl());
            } catch (ApiProxyException e) {
              e.printStackTrace();
            }
            return null;
          }
        };

    Thread t = new Thread(preloadTask);
    t.setDaemon(true);
    t.start();
  }

  /**
   * Speak using a cached audio URL if available to reduce latency; otherwise fall back to full
   * speak.
   */
  public static void speakCached(String text) {
    if (text == null || text.isEmpty()) return;
    String audioUrl = audioCache.get(text);
    if (audioUrl != null) {
      // Play cached URL on background thread
      Task<Void> playTask =
          new Task<>() {
            @Override
            protected Void call() {
              try (InputStream inputStream =
                  new BufferedInputStream(URI.create(audioUrl).toURL().openStream())) {
                Player player = new Player(inputStream);
                player.play();
              } catch (JavaLayerException | IOException e) {
                e.printStackTrace();
              }
              return null;
            }
          };

      Thread t = new Thread(playTask);
      t.setDaemon(true);
      t.start();
    } else {
      // Not cached yet, generate then play using full flow which will also cache result via preload
      // if desired
      speak(text);
    }
  }
}
