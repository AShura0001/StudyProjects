package nz.ac.auckland.se281.engine;

import nz.ac.auckland.se281.Main.Difficulty;
import nz.ac.auckland.se281.cli.MessageCli;
import nz.ac.auckland.se281.cli.Utils;

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
    // "Starting round %s of %s:"
    if (currentRound != null && totalRounds != null) {
      MessageCli.START_ROUND.printMessage(currentRound, totalRounds);
      currentRound++;

      MessageCli.ASK_HUMAN_INPUT.printMessage();
      String humanInput = Utils.scanner.nextLine();
    }
  }

  public void showStats() {}
}
