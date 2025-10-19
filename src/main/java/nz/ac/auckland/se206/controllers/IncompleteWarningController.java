package nz.ac.auckland.se206.controllers;

import java.util.Set;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import nz.ac.auckland.se206.controllers.ChatController.Participant;

/** Controller for the incomplete investigation warning overlay. */
public class IncompleteWarningController {

  @FXML private Label messageLabel;
  @FXML private VBox participantList;

  private RoomFrameController frame;

  public void setFrame(RoomFrameController frame) {
    this.frame = frame;
  }

  /**
   * Sets the list of uncontacted participants to display.
   *
   * @param uncontacted Set of participants the player hasn't contacted yet
   */
  public void setUncontactedParticipants(Set<Participant> uncontacted) {
    if (participantList == null) return;

    participantList.getChildren().clear();

    for (Participant p : uncontacted) {
      String displayName = getParticipantDisplayName(p);
      Label participantLabel = new Label("• " + displayName);
      participantLabel.setStyle(
          "-fx-font-size: 15px; -fx-text-fill: #991B1B; -fx-font-weight: 600;");
      participantList.getChildren().add(participantLabel);
    }
  }

  @FXML
  private void onClose() {
    // Hide the warning overlay
    if (frame != null) {
      frame.hideWarning();
    }
  }

  private String getParticipantDisplayName(Participant p) {
    switch (p) {
      case AI_WITNESS:
        return "Sentri-Guard (AI Witness)";
      case HUMAN_WITNESS:
        return "Casey (Human Witness)";
      case DEFENDANT:
        return "Cookie Monster v2.3 (Defendant)";
      default:
        return p.name();
    }
  }
}
