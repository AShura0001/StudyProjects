package nz.ac.auckland.se281;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {
  private Map<Integer, List<Integer>> adjNodes;

  public Graph() {
    adjNodes = new HashMap<>();
  }
}
