package nz.ac.auckland.se281;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph {
  private Map<Country, List<Country>> adjNodes;

  public Graph() {
    adjNodes = new HashMap<>();
  }

  public void addNodes(Country rooCountry, List<Country> adjCountries) {
    adjNodes.put(rooCountry, adjCountries);
  }

  public Set<String> getNeighbours(Country rootCountry) {
    Set<String> neighbourSet = new LinkedHashSet<>();
    for (Country neighbour : adjNodes.get(rootCountry)) {
      neighbourSet.add(neighbour.getName());
    }
    return neighbourSet;
  }
}
