package nz.ac.auckland.se281;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {
  private Map<Country, List<Country>> adjNodes;

  public Graph() {
    adjNodes = new HashMap<>();
  }

  public void addNodes(Country rooCountry, List<Country> adjCountries) {
    adjNodes.put(rooCountry, adjCountries);
  }
}
