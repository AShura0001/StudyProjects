package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class FlashbackController {
  private RoomFrameController parent;
  @FXML private VBox adContainer;
  @FXML private Button btnAccept;
  @FXML private Button btnExit;
  @FXML private Label adMessage;

  private int exitAttempts = 0;
  private static final int EXIT_BLOCKS = 5;
  private boolean flashbackAnnounced = false;

  public void setParent(RoomFrameController parent) {
    this.parent = parent;
  }

  public void setGameStateContext(GameStateContext gameStateContext) {
    this.gameStateContext = gameStateContext;
    if (gameStateContext != null && gameStateContext.getChatController() != null) {
      // Connect flashback chat widgets to the main ChatController
      gameStateContext
          .getChatController()
          .setFlashbackChatComponents(lblStatus, txtaChat, txtInput, btnSend);
    }
  }

  @FXML
  private void onBack() {
    if (parent != null) {
      parent.backToRoom();
    }
  }

  /** Delegate the flashback chat send to ChatController. */
  @FXML
  private void onSendMessage() {
    if (gameStateContext != null && gameStateContext.getChatController() != null) {
      gameStateContext.getChatController().onSendMessage();
    }
  }

  @FXML
  private void initialize() {
    if (btnAccept != null) {
      btnAccept.setOnAction(
          e -> {
            announceFlashbackIfFirstTime();
          });
    }

    if (btnExit != null) {
      btnExit.setOnAction(
          e -> {
            exitAttempts++;
            if (exitAttempts < EXIT_BLOCKS) {
              btnExit.setText("Try again (" + (EXIT_BLOCKS - exitAttempts) + ")");
              if (adMessage != null) {
                adMessage.setText("Ad refuses to close. Please accept cookies or try again.");
              }
            } else {
              announceFlashbackIfFirstTime();
              btnExit.setDisable(true);
            }
          });
    }
  }
    ChatController.announceFlashbackViewedStatic(ChatController.Participant.DEFENDANT);
  }
}
