package nz.ac.auckland.se281.engine;

import nz.ac.auckland.se281.model.Colour;

class RandomStrategy implements Strategy {
  @Override
  public Colour chooseColor() {
    return Colour.getRandomColourForAi();
  }
}
