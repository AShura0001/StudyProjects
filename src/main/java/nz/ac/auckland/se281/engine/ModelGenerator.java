package nz.ac.auckland.se281.engine;

import nz.ac.auckland.se281.Main.Difficulty;
import nz.ac.auckland.se281.cli.MessageCli;

public class ModelGenerator {
  // This class is used to generate the AI model based on the difficulty level.
  // Basically instantiates the model and returns it.
  public static AiModel createModel(Difficulty difficulty) {
    // Uses switch cases to determine what kind of model to instantiate.
    switch (difficulty) {
      case EASY:
        return new EasyModel();
      case MEDIUM:
        return new MediumModel();
      case HARD:
        return new HardModel();
      default:
        // if the difficulty is not recognised, print an error message and return null.
        MessageCli.INVALID_DIFFICULTY.printMessage();
        return null;
    }
  }
}
