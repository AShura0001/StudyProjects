package nz.ac.auckland.se281.engine;

import nz.ac.auckland.se281.Main.Difficulty;
import nz.ac.auckland.se281.cli.MessageCli;

public class ModelGenerator {
  public static AiModel createModel(Difficulty difficulty) {
    switch (difficulty) {
      case EASY:
        return new EasyModel();
      case MEDIUM:
        return new MediumModel();
      case HARD:
        return new HardModel();
      default:
        MessageCli.INVALID_DIFFICULTY.printMessage();
    }
    return null;
  }
}
