package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

public interface Strategy {
  // Blueprint for the strategies [Random, AvoidLast, LeastUsed]
  Colour chooseColor(ArrayList<Colour> memory);
}
