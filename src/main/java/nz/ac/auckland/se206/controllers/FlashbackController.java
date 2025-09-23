package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import nz.ac.auckland.se206.GameStateContext;

public class FlashbackController {
  private RoomFrameController parent;
  @FXML private VBox adContainer;
  @FXML private Button btnAccept;
  @FXML private Button btnExit;
  private int exitAttempts = 0;
  private static final int EXIT_BLOCKS = 5;
  @FXML private Label adMessage;

  // Chat components
  @FXML private Label lblStatus;
  @FXML private TextArea txtaChat;
  @FXML private TextField txtInput;
  @FXML private Button btnSend;

  private GameStateContext gameStateContext;

  public void setParent(RoomFrameController parent) {
    this.parent = parent;
  }

  public void setGameStateContext(GameStateContext gameStateContext) {
    this.gameStateContext = gameStateContext;
    if (gameStateContext != null && gameStateContext.getChatController() != null) {
      // Connect chat components to the chat controller
      gameStateContext
          .getChatController()
          .setFlashbackChatComponents(lblStatus, txtaChat, txtInput, btnSend);
    }
  }

  @FXML
  private void onBack() {
    if (parent != null) parent.backToRoom();
  }

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
            if (adContainer != null) adContainer.setVisible(false);
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
              if (adContainer != null) adContainer.setVisible(false);
            }
          });
    }
  }
}
