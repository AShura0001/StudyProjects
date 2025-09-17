package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class RoomController {

  // Image and status
  @FXML
  private ImageView roomImage;
  @FXML
  private Label sceneStatus;
  @FXML
  private ListView<String> transcriptView;

  // Optional footer/header controls (keep only if present in FXML)
  @FXML
  private Button btnCallVote;
  @FXML
  private Button btnStartBriefing;
  @FXML
  private Button btnSummarize;
  @FXML
  private ChoiceBox<String> focusChoice;

  // Preloaded images
  private Image imgDefault;
  private Image imgA;
  private Image imgB;
  private Image imgC;

  @FXML
  private StackPane imageHost; // The parent StackPane in FXML

  @FXML
  private void initialize() {
    // Load images from classpath. Prefer filenames without spaces.
    imgDefault = load("/images/courtRoom.png");

    // If your files currently include spaces, either rename the files (recommended)
    // flashback1-human.png
    // flashback2-aiWitness.png
    // flashback3-cookieMonster.png
    // and then use those names below:
    imgA = load("/images/flashback1-human.png");
    imgB = load("/images/flashback2-aiWitness.png");
    imgC = load("/images/flashback3-cookieMonster.png");

    // Fallback: if you cannot rename the files, uncomment these lines instead:
    // imgA = load("/images/flashback1 - human.png");
    // imgB = load("/images/flashback2 - aiWitness.png");
    // imgC = load("/images/flashback3 - cookieMonster.png");

    // Ensure default image is shown at startup (also set in FXML)
    if (roomImage != null && imgDefault != null) {
      roomImage.setImage(imgDefault);
      roomImage.setPreserveRatio(false);
    }

    // Bind ImageView size to parent StackPane
    roomImage.fitWidthProperty().bind(imageHost.widthProperty());
    roomImage.fitHeightProperty().bind(imageHost.heightProperty());
    roomImage.setPreserveRatio(false);

    // Seed choice box items if control exists and items weren’t declared in FXML
    if (focusChoice != null && focusChoice.getItems().isEmpty()) {
      focusChoice.getItems().addAll("System Safety", "Human Factors", "Intent/Ethics");
    }

    setStatus("Click a person to interact.");

  }

  // Add parent reference for navigation
  private RoomFrameController parent;
  public void setParent(RoomFrameController parent) { this.parent = parent; }

  // Hotspot handlers (wired from FXML onAction)
  @FXML
  private void onHotspotA() {
    // Either do in-place image swap OR navigate to flashback
    if (parent != null) {
      parent.showFlashback(1);
    } else {
      swap(imgA, "Focused on Participant A…");
    }
    // Hotspot A is Human Witness
    var gameState = nz.ac.auckland.se206.App.getGameStateContext();
    if (gameState != null) {
      gameState.focusChatForParticipant(nz.ac.auckland.se206.controllers.ChatController.Participant.HUMAN_WITNESS);
      gameState.requestOpeningMessage(nz.ac.auckland.se206.controllers.ChatController.Participant.HUMAN_WITNESS);
    }
  }

  @FXML
  private void onHotspotB() {
    if (parent != null) {
      parent.showFlashback(2);
    } else {
      swap(imgB, "Focused on Participant B…");
    }
    var gameState = nz.ac.auckland.se206.App.getGameStateContext();
    if (gameState != null) {
      gameState.focusChatForParticipant(nz.ac.auckland.se206.controllers.ChatController.Participant.AI_WITNESS);
      gameState.requestOpeningMessage(nz.ac.auckland.se206.controllers.ChatController.Participant.AI_WITNESS);
    }
  }

  @FXML
  private void onHotspotC() {
    if (parent != null) {
      parent.showFlashback(3);
    } else {
      swap(imgC, "Focused on Participant C…");
    }
    // Hotspot C is Defendant
    var gameState = nz.ac.auckland.se206.App.getGameStateContext();
    if (gameState != null) {
      gameState.focusChatForParticipant(nz.ac.auckland.se206.controllers.ChatController.Participant.DEFENDANT);
      gameState.requestOpeningMessage(nz.ac.auckland.se206.controllers.ChatController.Participant.DEFENDANT);
    }
  }

  // Optional header/footer actions (wire in FXML if you use them)
  @FXML
  private void onCallVote() {
    appendNote("Vote called.");
    setStatus("Calling a vote…");
  }

  @FXML
  private void onStartBriefing() {
    appendNote("Briefing started.");
    setStatus("Briefing in progress.");
  }

  @FXML
  private void onSummarize() {
    appendNote("Summary requested.");
    setStatus("Summarizing conversation…");
  }

  // Helpers
  @SuppressWarnings("unused")
  private void showDefault() {
    if (imgDefault != null) {
      roomImage.setImage(imgDefault);
      roomImage.setPreserveRatio(false);
      roomImage.setSmooth(true);
    }
    setStatus("Click a person to interact.");
  }

  private void swap(Image target, String status) {
    if (target != null) {
      roomImage.setImage(target);
      roomImage.setPreserveRatio(false);
      roomImage.setSmooth(true);
    } else {
      // If target failed to load, stay on current image and log
      appendNote("Image not found for this interaction.");
    }
    appendNote(status);
    setStatus(status);
  }

  private void appendNote(String text) {
    if (transcriptView != null) {
      transcriptView.getItems().add(text);
      transcriptView.scrollTo(transcriptView.getItems().size() - 1);
    }
  }

  private void setStatus(String text) {
    if (sceneStatus != null) {
      sceneStatus.setText(text);
    }
  }

  private Image load(String cp) {
    try {
      var url = getClass().getResource(cp);
      if (url == null) {
        System.err.println("Resource not found: " + cp);
        return null;
      }
      return new Image(url.toExternalForm(), true);
    } catch (Exception e) {
      System.err.println("Failed to load: " + cp + " -> " + e.getMessage());
      return null;
    }
  }
}