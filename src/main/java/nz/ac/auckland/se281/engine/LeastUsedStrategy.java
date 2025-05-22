package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

public class LeastUsedStrategy implements Strategy {
  private ArrayList<Integer> colourCount = new ArrayList<>();
  private int leastUsedColourIndex = 0;

  public LeastUsedStrategy() {
    colourCount.add(0); // RED
    colourCount.add(0); // YELLOW
    colourCount.add(0); // GREEN
    colourCount.add(0); // BLUE
  }

  @Override
  public Colour chooseColor(ArrayList<Colour> memory) {
    for (Colour colour : memory) {
      switch (colour) {
        case RED:
          colourCount.set(0, colourCount.get(0) + 1);
          break;
        case YELLOW:
          colourCount.set(1, colourCount.get(1) + 1);
          break;
        case GREEN:
          colourCount.set(2, colourCount.get(2) + 1);
          break;
        case BLUE:
          colourCount.set(3, colourCount.get(3) + 1);
          break;
      }
    }

    for (int i = 0; i < colourCount.size(); i++) {
      if (colourCount.get(i) < colourCount.get(leastUsedColourIndex)) {
        leastUsedColourIndex = i;
      }
    }
    if (leastUsedColourIndex == 0) {
      return Colour.RED;
    } else if (leastUsedColourIndex == 1) {
      return Colour.YELLOW;
    } else if (leastUsedColourIndex == 2) {
      return Colour.GREEN;
    } else {
      return Colour.BLUE;
    }
  }
}
