package lavs.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import lavs.ui.Navigator;

public class LoggedOutController {

    @FXML private Button loginAgainButton;

    @FXML
    private void initialize() {
        loginAgainButton.setDefaultButton(true);
    }

    @FXML
    private void onLoginAgain() {
        Navigator.show("login");
    }
}
