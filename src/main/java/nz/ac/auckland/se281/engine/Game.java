package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.Main.Difficulty;
import nz.ac.auckland.se281.cli.MessageCli;
import nz.ac.auckland.se281.cli.Utils;
import nz.ac.auckland.se281.model.Colour;

public class Game {

  public static String AI_NAME = "HAL-9000";

  private AiModel currentModel;
  private ArrayList<Colour> memory = new ArrayList<>(); // Stores players moves locally
  private Boolean gameOn = false;
  private Boolean winFlag = false;
  private Integer aiScore = 0;
  private Integer currentRoundNum;
  private Integer totalRounds;
  private Integer userScore = 0;
  private String playerName;

  private void endResult() {
    // Prints the final score and the winner
    MessageCli.PRINT_PLAYER_POINTS.printMessage(playerName, userScore);
    MessageCli.PRINT_PLAYER_POINTS.printMessage(AI_NAME, aiScore);
    MessageCli.PRINT_END_GAME.printMessage();
    if (userScore > aiScore) {
      MessageCli.PRINT_WINNER_GAME.printMessage(playerName);
    } else if (userScore < aiScore) {
      MessageCli.PRINT_WINNER_GAME.printMessage(AI_NAME);
    } else {
      MessageCli.PRINT_TIE_GAME.printMessage();
    }
  }

  public void newGame(Difficulty difficulty, int numRounds, String[] options) {
    // Starts a new game with given parameters
    this.playerName = options[0];
    MessageCli.WELCOME_PLAYER.printMessage(playerName);
    if (numRounds > 0) {
      // ensure that the number of rounds is a positive integer
      // Apparently its implemeted from the start and we don't need to check it
      this.totalRounds = numRounds;
    } else {
      MessageCli.INVALID_ROUNDS.printMessage();
    }
    // Either Initializes the following varaibles
    // or resets them for a new game as per the requirements
    this.currentRoundNum = 1;
    this.currentModel = ModelGenerator.createModel(difficulty);
    // Player moves are reset for a new game
    memory.clear();
    winFlag = false;
    userScore = 0;
    aiScore = 0;
    gameOn = true;
  }

  public void play() {
    // plays a round of the game
    if (gameOn) {
      // Only plays a round if the game is on
      // <chosen colour, guessed colour>
      ArrayList<Colour> userChosenColours = new ArrayList<>();
      ArrayList<Colour> aiChosenColours = new ArrayList<>();
      Integer userPointearned = 0;
      Integer aiPointearned = 0;
      // Colour chosenColour = new Colour();
      // Checks if curentRoundNum and totalRounds are valid
      if (currentRoundNum != null && totalRounds != null) {
        // "Starting round %s of %s:"
        MessageCli.START_ROUND.printMessage(currentRoundNum, totalRounds);
        // local flag to make while loop work, so that player can input
        // invalid input as many times until valid input is given
        Boolean inputFlag = true;

        while (inputFlag) {
          MessageCli.ASK_HUMAN_INPUT.printMessage();
          // Reads the input from the user
          String humanInput = Utils.scanner.nextLine();
          String[] choices = humanInput.split(" ");
          if (choices.length == 2) {
            for (String choice : choices) {
              if (Colour.fromInput(choice) != null) {
                // If input is valid, turn flag off since till now no
                // invalid input was given and new inputs are not required
                userChosenColours.add(Colour.fromInput(choice));
                inputFlag = false;
              } else {
                // Invalid input found.
                // turn the flag on since we required new inputs
                // clear the prior inputs incase one of them was valid and stored
                MessageCli.INVALID_HUMAN_INPUT.printMessage();
                userChosenColours.clear();
                inputFlag = true;
                // for loop is broken at the first invalid input to ensure the second
                // input is not checked
                break;
              }
            }
          } else {
            MessageCli.INVALID_HUMAN_INPUT.printMessage();
          }
        }

        // calls the moves from respective ai model and stored
        // as current round's ai moves.
        aiChosenColours.add(currentModel.choose());
        aiChosenColours.add(currentModel.guess(currentRoundNum, memory, winFlag));

        // formats the moves based on who had turn (unnecessary)
        if ((currentRoundNum % 2) == 0) {
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

        // checks if it is a 3rd round
        // <chosen colour, guessed colour>
        if ((currentRoundNum % 3) == 0) {
          // chooses a random power colour for current round
          Colour powerColour = Colour.getRandomColourForPowerColour();
          MessageCli.PRINT_POWER_COLOUR.printMessage(powerColour);
          // checks if the power colour is same as guessed colour for both ai and user
          // seperately and assigns points accordingly.
          if (userChosenColours.get(1) == powerColour) {
            if (userChosenColours.get(1) == aiChosenColours.get(0)) {
              userPointearned = 3;
            }
          } else if (userChosenColours.get(1) == aiChosenColours.get(0)) {
            userPointearned = 1;
          }
          if (aiChosenColours.get(1) == powerColour) {
            if (aiChosenColours.get(1) == userChosenColours.get(0)) {
              aiPointearned = 3;
            }
          } else if (aiChosenColours.get(1) == userChosenColours.get(0)) {
            aiPointearned = 1;
          }
        } else {
          // checks if guessed colour is right but not a power colour round.
          if (userChosenColours.get(1) == aiChosenColours.get(0)) {
            userPointearned = 1;
          }
          if (aiChosenColours.get(1) == userChosenColours.get(0)) {
            aiPointearned = 1;
          }
        }

        // updates the stats of the game
        userScore += userPointearned;
        aiScore += aiPointearned;
        // stores the result for ai for each round which might be needed if
        // the ai model is set to hard, as the strategy switches on a loss after 3 round.
        if (aiPointearned != 0) {
          winFlag = true;
        } else {
          winFlag = false;
        }
        // prints the result of each individual round.
        // "Player %s earned %s point(s) this round."
        MessageCli.PRINT_OUTCOME_ROUND.printMessage(playerName, userPointearned);
        MessageCli.PRINT_OUTCOME_ROUND.printMessage(AI_NAME, aiPointearned);
        memory.add(userChosenColours.get(0));
        if (currentRoundNum == totalRounds) {
          // if last round is reached, ends game and prints the final result.
          endResult();
          gameOn = false;
        } else {
          // if not, then passes to next round.
          currentRoundNum++;
        }
      }
    } else {
      // If game is not started, play cannot be called
      MessageCli.GAME_NOT_STARTED.printMessage();
    }
  }

  public void showStats() {
    // Prints current stats of the game only if the game is on
    if (gameOn) {
      // "Player %s has %s point(s)."
      MessageCli.PRINT_PLAYER_POINTS.printMessage(playerName, userScore);
      MessageCli.PRINT_PLAYER_POINTS.printMessage(AI_NAME, aiScore);
    } else {
      MessageCli.GAME_NOT_STARTED.printMessage();
    }
  }
}
