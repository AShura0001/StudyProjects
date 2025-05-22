package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

public interface Strategy {
  Colour chooseColor(ArrayList<Colour> memory);
}
