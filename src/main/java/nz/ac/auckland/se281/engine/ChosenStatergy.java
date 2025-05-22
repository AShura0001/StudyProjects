package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

public class ChosenStatergy {
  Strategy chosernStrategy;

  public ChosenStatergy(Strategy reqStrategy) {
    this.chosernStrategy = reqStrategy;
  }

  public void setStrategy(Strategy reqStrategy) {
    this.chosernStrategy = reqStrategy;
  }

  public Colour execute(ArrayList<Colour> memory) {
    return chosernStrategy.chooseColor(memory);
  }
}
