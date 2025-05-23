package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

class EasyModel implements AiModel {
  // This is an easy AI model which uses random strategy to
  // choose as well as guess a colour.
  private ChosenStatergy guessStatergy = new ChosenStatergy(new RandomStrategy());

  @Override
  public Colour choose() {
    return Colour.getRandomColourForAi();
  }

  @Override
  // The parameters although taken have no effect on this AI model.
  public Colour guess(Integer currentRoundNum, ArrayList<Colour> memory, Boolean winFlag) {
    return guessStatergy.execute(memory);
  }
}
