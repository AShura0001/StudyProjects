package nz.ac.auckland.se281.engine;

import nz.ac.auckland.se281.model.Colour;

class EasyModel implements AiModel {

  public Colour choose() {
    return Colour.getRandomColourForAi();
  }

  @Override
  public Colour guess() {
    return Colour.getRandomColourForAi();
  }
}
