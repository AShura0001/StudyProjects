package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

public class AvoidLastStatergy implements Strategy {
  // Strategy that avoids the last played colour by the user
  @Override
  public Colour chooseColor(ArrayList<Colour> memory) {
    return Colour.getRandomColourExcluding(memory.get(memory.size() - 1));
  }
}
