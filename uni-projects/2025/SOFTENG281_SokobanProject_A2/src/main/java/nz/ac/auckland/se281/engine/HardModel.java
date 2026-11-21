package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

public class HardModel implements AiModel {
  // This is a hard AI model which uses random strategy to choose
  // but for guessing it uses a combination of strategies.
  private ChosenStatergy guessStatergy = new ChosenStatergy(new RandomStrategy());
  // this flag helps in recognizing the current strategy being used
  // between avoid last and least used strategy.
  private Boolean avlStratFlag = false;

  @Override
  public Colour choose() {
    return Colour.getRandomColourForAi();
  }

  @Override
  // The parameters are used to determine the strategy to be used/switched to.
  public Colour guess(Integer currentRoundNum, ArrayList<Colour> memory, Boolean winFlag) {
    // System.out.println("flag is" + winFlag);
    if (currentRoundNum < 3) {
      // for round 1 and 2, use random strategy (as initialized at the start)
      return guessStatergy.execute(memory);
    } else if (currentRoundNum == 3) {
      // for round 3, switch to least used strategy
      guessStatergy.setStrategy(new LeastUsedStrategy());
      return guessStatergy.execute(memory);
    } else {
      // for round 4 and above, switch between least used and avoid last strategy
      // based on the success of current strategy in previous round.
      if (winFlag) {
        // if the strategy was successful, continue using it.
        return guessStatergy.execute(memory);
      } else {
        // else switch to the other strategy.
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
