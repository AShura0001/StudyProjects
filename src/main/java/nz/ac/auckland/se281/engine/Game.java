package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.Main.Difficulty;
import nz.ac.auckland.se281.cli.MessageCli;
import nz.ac.auckland.se281.cli.Utils;
import nz.ac.auckland.se281.model.Colour;

public class Game {
  private Integer currentRound;
  private Integer totalRounds;

  public static String AI_NAME = "HAL-9000";

  public Game() {}

  public void newGame(Difficulty difficulty, int numRounds, String[] options) {
    String playerName = options[0];
    MessageCli.WELCOME_PLAYER.printMessage(playerName);
    this.totalRounds = numRounds;
    this.currentRound = 1;
  }

  public void play() {
    ArrayList<Colour> chosenColours = new ArrayList<>();
    // Colour chosenColour = new Colour();
    if (currentRound != null && totalRounds != null) {
      // "Starting round %s of %s:"
      MessageCli.START_ROUND.printMessage(currentRound, totalRounds);
      Boolean inputFlag = false;

      inputFlag = true;
      while (inputFlag) {
        MessageCli.ASK_HUMAN_INPUT.printMessage();
        String humanInput = Utils.scanner.nextLine();
        String[] choices = humanInput.split(" ");
        for (String choice : choices) {
          System.out.println(choice);
          // System.out.println(Colour.fromInput(choice));
          if (Colour.fromInput(choice) != null) {
            System.out.println(Colour.fromInput(choice));
            chosenColours.add(Colour.fromInput(choice));
          } else {
            MessageCli.INVALID_HUMAN_INPUT.printMessage();
            chosenColours.clear();
          }
        }
        inputFlag = false;
      }
      currentRound++;
    }
  }

  public void showStats() {}
}
