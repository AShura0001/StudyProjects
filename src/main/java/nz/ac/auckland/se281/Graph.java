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
    // Initialize the adjacent countries map
    adjCountriesMap = new HashMap<>();
  }

  public void addNodes(Country rooCountry, List<Country> adjCountries) {
    // adds a root country and its adjacent countries to the graph
    adjCountriesMap.put(rooCountry, adjCountries);
  }

  public Set<String> getNeighbours(Country rootCountry) {
    // returns a set of names of the adjacent countries of the root country
    Set<String> neighbourSet = new LinkedHashSet<>();
    for (Country neighbour : adjCountriesMap.get(rootCountry)) {
      neighbourSet.add(neighbour.getName());
    }
    return neighbourSet;
  }

  public List<Country> getFastestRouteBFS(Country startCountry, Country endCountry) {
    // A modified BFS algorithm to find the fastest route between two countries
    List<Country> visited = new ArrayList<>();
    Queue<Country> queue = new LinkedList<>();
    List<Country> path = new ArrayList<>();
    // A parent map to hold the link between each parent country and its child country
    // based on the traversal of BFS on adjacent countries map.
    // key: child country, value: parent country.
    Map<Country, Country> parentMap = new HashMap<>();

    // start country has no parent, so we put it as null in the parent map.
    parentMap.put(startCountry, null);
    queue.add(startCountry);
    visited.add(startCountry);
    while (!queue.isEmpty()) {
      Country localRootCountry = queue.poll();
      // if the local roots is the end country, we start backtracking the path
      if (localRootCountry.equals(endCountry)) {
        // backtrack the path from end country to start country using the parent map
        // until we reach the start country that has Null as its parent.
        for (Country countryOnRoute = endCountry;
            countryOnRoute != null;
            countryOnRoute = parentMap.get(countryOnRoute)) {
          path.add(countryOnRoute);
        }
        // reverse the path to get it from start to end country
        Collections.reverse(path);
      }
      for (Country branchCountry : adjCountriesMap.get(localRootCountry)) {
        // if the branch country is not visited, we add it to the queue and mark it as visited
        // and also put the local root country as its parent in the parent map.
        if (!visited.contains(branchCountry)) {
          visited.add(branchCountry);
          queue.add(branchCountry);
          parentMap.put(branchCountry, localRootCountry); //
        }
      }
    }
    return path; // return the shortest path from start to end country
  }
}
