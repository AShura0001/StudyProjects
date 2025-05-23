package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

public class ChosenStatergy {
  // This class creates a strategy object and also allows to switch 
  // during runtime between different strategies.
  private Strategy chosenStrategy;

  public ChosenStatergy(Strategy reqStrategy) {
    // Constructor to set the initial strategy
    this.chosenStrategy = reqStrategy;
  }

  public void setStrategy(Strategy reqStrategy) {
    this.chosenStrategy = reqStrategy;
  }

  public Colour execute(ArrayList<Colour> memory) {
    return chosenStrategy.chooseColor(memory);
  }
}
