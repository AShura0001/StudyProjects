package nz.ac.auckland.se281.engine;

import nz.ac.auckland.se281.model.Colour;

class EasyModel implements AiModel {
  ChosenStatergy guessStatergy = new ChosenStatergy(new RandomStrategy());
  ChosenStatergy chooseStatergy = new ChosenStatergy(new RandomStrategy());

  @Override
  public Colour guess() {
    return guessStatergy.execute();
  }

  @Override
  public Colour choose() {
    return chooseStatergy.execute();
  }
}
