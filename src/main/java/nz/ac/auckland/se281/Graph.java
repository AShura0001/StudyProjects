package nz.ac.auckland.se281;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Graph {
  private Map<Country, List<Country>> adjCountriesMap;

  public Graph() {
    adjCountriesMap = new HashMap<>();
  }

  public void addNodes(Country rooCountry, List<Country> adjCountries) {
    adjCountriesMap.put(rooCountry, adjCountries);
  }

  public Set<String> getNeighbours(Country rootCountry) {
    Set<String> neighbourSet = new LinkedHashSet<>();
    for (Country neighbour : adjCountriesMap.get(rootCountry)) {
      neighbourSet.add(neighbour.getName());
    }
    return neighbourSet;
  }

  public List<Country> getFastestRouteBFS(Country startCountry, Country endCountry) {
    List<Country> visited = new ArrayList<>();
    Queue<Country> queue = new LinkedList<>();
    List<Country> path = new ArrayList<>();
    Map<Country, Country> parentMap = new HashMap<>();

    parentMap.put(startCountry, null);
    queue.add(startCountry);
    visited.add(startCountry);
    while (!queue.isEmpty()) {
      Country localRootCountry = queue.poll();
      if (localRootCountry.equals(endCountry)) {
        for (Country countryOnRoute = endCountry;
            countryOnRoute != null;
            countryOnRoute = parentMap.get(countryOnRoute)) {
          path.add(countryOnRoute);
        }
        Collections.reverse(path);
      }
      for (Country branchCountry : adjCountriesMap.get(localRootCountry)) {
        if (!visited.contains(branchCountry)) {
          visited.add(branchCountry);
          queue.add(branchCountry);
          parentMap.put(branchCountry, localRootCountry); //
        }
      }
    }
    return path;
  }
}
