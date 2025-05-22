package nz.ac.auckland.se281.engine;

import nz.ac.auckland.se281.model.Colour;

public interface AiModel {
  Colour choose();

  Colour guess();

  void setStrategy(Strategy reqStrategy);
}
