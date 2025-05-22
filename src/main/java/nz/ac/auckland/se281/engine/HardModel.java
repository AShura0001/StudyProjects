package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

public class HardModel implements AiModel {
  ChosenStatergy guessStatergy = new ChosenStatergy(new RandomStrategy());
  private Boolean avlStratFlag = false;

  @Override
  public Colour choose() {
    return Colour.getRandomColourForAi();
  }

  @Override
  public Colour guess(Integer currentRoundNum, ArrayList<Colour> memory, Boolean winFlag) {
    if (currentRoundNum < 3) {
      return guessStatergy.execute(memory);
    } else if (currentRoundNum == 3) {
      guessStatergy.setStrategy(new LeastUsedStrategy());
      return guessStatergy.execute(memory);
    } else {
      if (winFlag) {
        return guessStatergy.execute(memory);
      } else {
        if (avlStratFlag) {
          avlStratFlag = false;
          guessStatergy.setStrategy(new LeastUsedStrategy());
          return guessStatergy.execute(memory);
        } else {
          avlStratFlag = true;
          guessStatergy.setStrategy(new AvoidLastStatergy());
          return guessStatergy.execute(memory);
        }
      }
    }
  }
}
