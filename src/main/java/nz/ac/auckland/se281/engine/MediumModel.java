package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

public class MediumModel implements AiModel {
  ChosenStatergy guessStatergy = new ChosenStatergy(new RandomStrategy());

  @Override
  public Colour choose() {
    return Colour.getRandomColourForAi();
  }

  @Override
  public Colour guess(Integer currentRoundNum, ArrayList<Colour> memory, Boolean winFlag) {
    if (currentRoundNum < 2) {
      return guessStatergy.execute(memory);
    } else {
      guessStatergy.setStrategy(new AvoidLastStatergy());
      return guessStatergy.execute(memory);
    }
  }
}
