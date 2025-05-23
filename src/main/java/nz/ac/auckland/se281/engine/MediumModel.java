package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

public class MediumModel implements AiModel {
  // This is a medium AI model which uses random strategy to choose a colour
  // and random + avoid last strategy to guess a colour.
  private ChosenStatergy guessStatergy = new ChosenStatergy(new RandomStrategy());

  @Override
  public Colour choose() {
    return Colour.getRandomColourForAi();
  }

  @Override
  public Colour guess(Integer currentRoundNum, ArrayList<Colour> memory, Boolean winFlag) {
    if (currentRoundNum < 2) {
      // for first round, use random strategy (as initialized at the start)
      return guessStatergy.execute(memory);
    } else {
      // from round 2 onwards, switch to avoid last strategy
      guessStatergy.setStrategy(new AvoidLastStatergy());
      return guessStatergy.execute(memory);
    }
  }
}
