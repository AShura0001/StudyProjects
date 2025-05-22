package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

public class ChosenStatergy {
  private Strategy chosenStrategy;

  public ChosenStatergy(Strategy reqStrategy) {
    this.chosenStrategy = reqStrategy;
  }

  public void setStrategy(Strategy reqStrategy) {
    this.chosenStrategy = reqStrategy;
  }

  public Colour execute(ArrayList<Colour> memory) {
    return chosenStrategy.chooseColor(memory);
  }
}
