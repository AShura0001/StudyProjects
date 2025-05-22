package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

class EasyModel implements AiModel {
  ChosenStatergy guessStatergy = new ChosenStatergy(new RandomStrategy());

  @Override
  public Colour choose() {
    return Colour.getRandomColourForAi();
  }

  @Override
  public Colour guess(Integer currentRoundNum, ArrayList<Colour> memory) {
    return guessStatergy.execute(memory);
  }
}
