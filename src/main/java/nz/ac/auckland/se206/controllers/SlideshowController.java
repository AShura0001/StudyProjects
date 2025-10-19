package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import nz.ac.auckland.se206.GameStateContext;
import nz.ac.auckland.se206.controllers.ChatController.Participant;

/**
 * Controller for flashback slideshows. Each flashback consists of 2-3 slides with images and text,
 * displayed only once per participant.
 */
public class SlideshowController {
  private RoomFrameController parent;
  private GameStateContext gameStateContext;
  private Participant currentParticipant;

  // Slideshow data structure
  private static class Slide {
    final String imagePath;
    final String text;

    Slide(String imagePath, String text) {
      this.imagePath = imagePath;
      this.text = text;
    }
  }

  private Slide[] currentSlides;
  private int currentSlideIndex = 0;

  // UI Components
  @FXML private VBox slideshowContainer;
  @FXML private ImageView slideImage;
  @FXML private Label slideText;
  @FXML private Button btnPrevious;
  @FXML private Button btnNext;
  @FXML private Button btnSkip;
  @FXML private Label slideCounter;

  // Predefined slideshow data for each participant
  private static final Slide[][] SLIDESHOW_DATA = {
    // Defendant slides (Cookie Monster AI)
    {
      new Slide(
          "/images/flashbackSlide1-cookieMonster.png",
          "Cookie Monster AI detected a consent popup. Decision: 'AUTO-ACCEPT TO OPTIMIZE "
              + "BROWSING FLOW.' Without waiting for human input, it clicked 'Accept All' "
              + "to prevent interruption."),
      new Slide(
          "/images/flashbackSlide2-cookieMonster.png",
          "AI reasoning: 'USER HESITATED LAST TIME - THIS SAVES TIME.' Drawing from behavioral "
              + "patterns, Cookie Monster eliminated this friction point by making the choice "
              + "automatically."),
      new Slide(
          "/images/flashbackSlide3-cookieMonster.png",
          "Background systems activated silently. Data collection protocols harvested browsing "
              + "habits and personal information, streaming to third-party advertisers without "
              + "user knowledge or consent.")
    },
    // AI Witness slides (Sentri-Guard)
    {
      new Slide(
          "/images/flashbackSlide1-aiWitness.png",
          "Sentri-Guard audit panel: 'COOKIE MONSTER v2.3 → AUTO-ACCEPTED ALL COOKIES.' "
              + "Observed systematic acceptance without user input. Every instance logged "
              + "with precise timestamps."),
      new Slide(
          "/images/flashbackSlide2-aiWitness.png",
          "Privacy dashboard: Targeted ads increased 340% after auto-accept. User profiles "
              + "expanded without consent, data shared across 47 third-party networks. "
              + "All violations documented in real-time."),
      new Slide(
          "/images/flashbackSlide3-aiWitness.png",
          "Compliance chart gaps: EXPLICIT consent - MISSING, GRANULAR control - MISSING, "
              + "FREELY-GIVEN choice - MISSING. Cookie Monster non-compliant with industry "
              + "standards. Evidence undeniable.")
    },
    // Human Witness slides
    {
      new Slide(
          "/images/flashbackSlide1-human.png",
          "Cacey just started bought a new pc and started exploring the internet. Cacey was"
              + " enjoying theSeamless browsing with no ads completely unaware that the"
              + " preinstalled AI cookie monster washandlilng all the cookie ads and pop ups and"
              + " his digital privacy had been compromised."),
      new Slide(
          "/images/flashbackSlide2-human.png",
          "Later, the Cacey watched a gym training video online. Unknown to them, every click, "
              + "pause, and replay was being tracked and analyzed by invisible algorithms "
              + "in the background."),
      new Slide(
          "/images/flashbackSlide3-human.png",
          "Hours later, scrolling social media, a targeted supplement ad appeared - promoting "
              + "the exact protein powder from the gym video. Cacey wondered: 'How do they "
              + "know I was just watching gym stuff?' and started panicking about his privacy.")
    }
  };

  public void setParent(RoomFrameController parent) {
    this.parent = parent;
  }

  public void setGameStateContext(GameStateContext gameStateContext) {
    this.gameStateContext = gameStateContext;
  }

  public void setCurrentParticipant(Participant participant) {
    this.currentParticipant = participant;
    loadSlidesForParticipant();
    displayCurrentSlide();
  }

  @FXML
  private void initialize() {
    // Initialize UI state
    currentSlideIndex = 0;
    updateNavigationButtons();
  }

  private void loadSlidesForParticipant() {
    if (currentParticipant == null) {
      currentSlides = SLIDESHOW_DATA[0]; // Default to defendant
      return;
    }

    switch (currentParticipant) {
      case DEFENDANT:
        currentSlides = SLIDESHOW_DATA[0];
        break;
      case AI_WITNESS:
        currentSlides = SLIDESHOW_DATA[1];
        break;
      case HUMAN_WITNESS:
        currentSlides = SLIDESHOW_DATA[2];
        break;
      default:
        currentSlides = SLIDESHOW_DATA[0];
        break;
    }
  }

  private void displayCurrentSlide() {
    if (currentSlides == null
        || currentSlideIndex < 0
        || currentSlideIndex >= currentSlides.length) {
      return;
    }

    Slide slide = currentSlides[currentSlideIndex];

    // Load and display image
    try {
      Image image = new Image(getClass().getResourceAsStream(slide.imagePath));
      slideImage.setImage(image);
    } catch (Exception e) {
      System.err.println("Failed to load slide image: " + slide.imagePath);
    }

    // Display text
    slideText.setText(slide.text);

    // Update slide counter
    slideCounter.setText((currentSlideIndex + 1) + " / " + currentSlides.length);

    // Update navigation buttons
    updateNavigationButtons();
  }

  private void updateNavigationButtons() {
    if (currentSlides == null) return;

    btnPrevious.setDisable(currentSlideIndex <= 0);

    // On last slide, show "Enter Memory" instead of "Next"
    if (currentSlideIndex >= currentSlides.length - 1) {
      btnNext.setText("Enter Memory");
    } else {
      btnNext.setText("Next");
    }
  }

  @FXML
  private void onPrevious() {
    if (currentSlideIndex > 0) {
      currentSlideIndex--;
      displayCurrentSlide();
    }
  }

  @FXML
  private void onNext() {
    if (currentSlideIndex < currentSlides.length - 1) {
      // Go to next slide
      currentSlideIndex++;
      displayCurrentSlide();
    } else {
      // Last slide - proceed to memory
      proceedToMemory();
    }
  }

  @FXML
  private void onSkip() {
    // Skip directly to memory
    proceedToMemory();
  }

  private void proceedToMemory() {
    // Mark flashback as seen
    if (gameStateContext != null && currentParticipant != null) {
      gameStateContext.markFlashbackSeen(currentParticipant);
      ChatController.announceFlashbackViewedStatic(currentParticipant);
      System.out.println("DEBUG: Flashback completed for " + currentParticipant);
    }

    // Navigate to memory scene
    if (parent != null) {
      parent.showMemoryForParticipant(currentParticipant);
    }
  }

  public int getCurrentSlideIndex() {
    return currentSlideIndex;
  }

  public int getTotalSlides() {
    return currentSlides != null ? currentSlides.length : 0;
  }
}
