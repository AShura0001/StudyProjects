package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

public class AvoidLastStatergy implements Strategy {

  @Override
  public Colour chooseColor(ArrayList<Colour> memory) {
    return Colour.getRandomColourExcluding(memory.get(memory.size()));
  }
}
