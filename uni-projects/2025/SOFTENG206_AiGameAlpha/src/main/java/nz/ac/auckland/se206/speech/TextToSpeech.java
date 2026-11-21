package nz.ac.auckland.se206.speech;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
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

  private static Thread currentSpeechThread;
  private static Player currentPlayer;
  private static final AtomicBoolean shouldStop = new AtomicBoolean(false);

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

    // Stop any currently playing speech
    stop();

    // Reset stop flag
    shouldStop.set(false);

    Task<Void> backgroundTask =
        new Task<>() {
          @Override
          protected Void call() {
            try {
              // Check if we should stop before starting
              if (shouldStop.get()) {
                return null;
              }

              ApiProxyConfig config = ApiProxyConfig.readConfig();
              Provider provider = Provider.GOOGLE;
              Voice voice = Voice.GOOGLE_EN_US_STANDARD_H;

              TextToSpeechRequest ttsRequest = new TextToSpeechRequest(config);
              ttsRequest.setText(text).setProvider(provider).setVoice(voice);

              TextToSpeechResult ttsResult = ttsRequest.execute();
              String audioUrl = ttsResult.getAudioUrl();

              // Check again before playing
              if (shouldStop.get()) {
                return null;
              }

              try (InputStream inputStream =
                  new BufferedInputStream(URI.create(audioUrl).toURL().openStream())) {
                currentPlayer = new Player(inputStream);

                // Check one more time before playing
                if (!shouldStop.get()) {
                  currentPlayer.play();
                }
              } catch (JavaLayerException | IOException e) {
                if (!shouldStop.get()) {
                  e.printStackTrace();
                }
              } finally {
                currentPlayer = null;
              }

            } catch (ApiProxyException e) {
              if (!shouldStop.get()) {
                e.printStackTrace();
              }
            }
            return null;
          }
        };

    currentSpeechThread = new Thread(backgroundTask);
    currentSpeechThread.setDaemon(true);
    currentSpeechThread.start();
  }

  /** Stops any currently playing speech */
  public static void stop() {
    // Set stop flag to prevent new speech from starting
    shouldStop.set(true);

    // Close current player
    if (currentPlayer != null) {
      try {
        currentPlayer.close();
      } catch (Exception e) {
        // Ignore close exceptions
      }
      currentPlayer = null;
    }

    // Interrupt current thread
    if (currentSpeechThread != null && currentSpeechThread.isAlive()) {
      currentSpeechThread.interrupt();
      try {
        currentSpeechThread.join(100); // Wait up to 100ms for thread to finish
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      currentSpeechThread = null;
    }
  }
}
