package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

class RandomStrategy implements Strategy {
  @Override
  public Colour chooseColor(ArrayList<Colour> memory) {
    return Colour.getRandomColourForAi();
  }
}
