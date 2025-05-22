package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.Main.Difficulty;
import nz.ac.auckland.se281.cli.MessageCli;
import nz.ac.auckland.se281.cli.Utils;
import nz.ac.auckland.se281.model.Colour;

public class Game {
  private Integer currentRound;
  private Integer totalRounds;
  private String playerName;
  private Difficulty currentDifficulty;

  public static String AI_NAME = "HAL-9000";

  public Game() {}

  public void newGame(Difficulty difficulty, int numRounds, String[] options) {
    this.playerName = options[0];
    MessageCli.WELCOME_PLAYER.printMessage(playerName);
    if (numRounds > 0) {
      this.totalRounds = numRounds;
    } else {
      MessageCli.INVALID_ROUNDS.printMessage();
    }
    this.currentRound = 1;
    this.currentDifficulty = difficulty;
  }

  public void play() {
    // <chosen colour, guessed colour>
    ArrayList<Colour> userChosenColours = new ArrayList<>();
    ArrayList<Colour> aiChosenColours = new ArrayList<>();
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
        if (choices.length == 2) {
          for (String choice : choices) {
            // System.out.println(choice);
            // System.out.println(Colour.fromInput(choice));
            if (Colour.fromInput(choice) != null) {
              // System.out.println("lola");
              // System.out.println(Colour.fromInput(choice));
              userChosenColours.add(Colour.fromInput(choice));
              inputFlag = false;
            } else {
              // System.out.println("brub");
              MessageCli.INVALID_HUMAN_INPUT.printMessage();
              userChosenColours.clear();
              inputFlag = true;
              break;
            }
          }
        } else {
          MessageCli.INVALID_HUMAN_INPUT.printMessage();
        }
      }

      if ((currentRound % 3) == 0) {
        Colour powerColour = Colour.getRandomColourForPowerColour();
        MessageCli.PRINT_POWER_COLOUR.printMessage(powerColour);
      }

      AiModel currentModel = ModelGenerator.createModel(currentDifficulty);
      aiChosenColours.add(currentModel.choose());
      aiChosenColours.add(currentModel.guess());

      if ((currentRound % 2) == 0) {
        // "Player %s: chose %s and guessed %s"
        MessageCli.PRINT_INFO_MOVE.printMessage(
            AI_NAME, aiChosenColours.get(0), aiChosenColours.get(1));
        MessageCli.PRINT_INFO_MOVE.printMessage(
            playerName, userChosenColours.get(0), userChosenColours.get(1));
      } else {
        MessageCli.PRINT_INFO_MOVE.printMessage(
            playerName, userChosenColours.get(0), userChosenColours.get(1));
        MessageCli.PRINT_INFO_MOVE.printMessage(
            AI_NAME, aiChosenColours.get(0), aiChosenColours.get(1));
      }

      // <chosen colour, guessed colour>
      if (userChosenColours.get(1) == aiChosenColours.get(0)) {
        // "Player %s earned %s point(s) this round."
        MessageCli.PRINT_OUTCOME_ROUND.printMessage(playerName, 1);
      } else {
        MessageCli.PRINT_OUTCOME_ROUND.printMessage(playerName, 0);
      }
      if (aiChosenColours.get(1) == userChosenColours.get(0)) {
        MessageCli.PRINT_OUTCOME_ROUND.printMessage(AI_NAME, 1);
      } else {
        MessageCli.PRINT_OUTCOME_ROUND.printMessage(AI_NAME, 0);
      }

      currentRound++;
    }
  }

  public void showStats() {}
}
