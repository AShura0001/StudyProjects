package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

public class LeastUsedStrategy implements Strategy {
  // This strategy chooses the least used colour by the user.
  // It keeps track of the number of times each colour has been used
  private ArrayList<Integer> colourCount = new ArrayList<>();
  private int leastUsedColourIndex = 0;

  public LeastUsedStrategy() {
    // Initialising the colourCount array with 0 for each colour
    // The following order is maintained:
    // RED, GREEN, BLUE, YELLOW
    colourCount.add(0); // RED
    colourCount.add(0); // GREEN
    colourCount.add(0); // BLUE
    colourCount.add(0); // YELLOW
    // Canonical order is maintained for the case when the current colour
    // and next colour have same count, in which case the one that comes
    // first is kept as least.
  }

  private void resetColourCount() {
    // Resetting the colourCount array to 0 for each colour.
    // Helps avoid repetition of the older moves being added to the memory
    // when we continue to use the same strategy.
    for (int i = 0; i < colourCount.size(); i++) {
      colourCount.set(i, 0);
    }
  }

  @Override
  public Colour chooseColor(ArrayList<Colour> memory) {
    // Fully goes through the memory each time the strategy is called
    // and counts the number of times each colour has been used.
    // increments the count of the colour with respective index
    for (Colour colour : memory) {
      switch (colour) {
        case RED:
          colourCount.set(0, colourCount.get(0) + 1);
          break;
        case GREEN:
          colourCount.set(1, colourCount.get(1) + 1);
          break;
        case BLUE:
          colourCount.set(2, colourCount.get(2) + 1);
          break;
        case YELLOW:
          colourCount.set(3, colourCount.get(3) + 1);
          break;
      }
    }

    for (int i = 0; i < colourCount.size(); i++) {
      // sets the least used colour index for the least used colour
      // initially set to 0 (RED)
      if (colourCount.get(i) < colourCount.get(leastUsedColourIndex)) {
        leastUsedColourIndex = i;
      }
    }

    // returns the least used colour based on the index
    // and resets the colourCount array to 0 for each colour.
    if (leastUsedColourIndex == 0) {
      resetColourCount();
      leastUsedColourIndex = 0;
      return Colour.RED;
    } else if (leastUsedColourIndex == 1) {
      resetColourCount();
      leastUsedColourIndex = 0;
      return Colour.GREEN;
    } else if (leastUsedColourIndex == 2) {
      resetColourCount();
      leastUsedColourIndex = 0;
      return Colour.BLUE;
    } else {
      resetColourCount();
      leastUsedColourIndex = 0;
      return Colour.YELLOW;
    }
  }
}
