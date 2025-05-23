package nz.ac.auckland.se281.engine;

import java.util.ArrayList;
import nz.ac.auckland.se281.model.Colour;

public interface AiModel {
  // Blueprint for the AI models based on difficulty [Easy, Medium, Hard]
  Colour choose();

  Colour guess(Integer currentRoundNum, ArrayList<Colour> memory, Boolean winFlag);
}
