package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

public class MediumModel implements AiModel {
  ChosenStatergy guessStatergy = new ChosenStatergy(new RandomStrategy());
  ChosenStatergy chooseStatergy = new ChosenStatergy(new RandomStrategy());

  @Override
  public Colour choose() {
    return Colour.getRandomColourForAi();
  }

  @Override
  public Colour guess(Integer currentRoundNum, ArrayList<Colour> memory) {
    if (currentRoundNum < 2) {
      return chooseStatergy.execute(memory);
    } else {
      chooseStatergy.setStrategy(new AvoidLastStatergy());
      return chooseStatergy.execute(memory);
    }
  }
}
