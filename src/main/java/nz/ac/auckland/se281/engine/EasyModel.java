package nz.ac.auckland.se281.engine;

import nz.ac.auckland.se281.model.Colour;

class EasyModel implements AiModel {
  private Strategy guessStrategy;
  private Strategy chooseStrategy = new RandomStrategy();

  @Override
  public Colour choose() {
    return Colour.getRandomColourForAi();
  }

  @Override
  public Colour guess() {
    return Colour.getRandomColourForAi();
  }

  @Override
  public void setStrategy(Strategy reqStrategy) {
    this.guessStrategy = reqStrategy;
  }
}
