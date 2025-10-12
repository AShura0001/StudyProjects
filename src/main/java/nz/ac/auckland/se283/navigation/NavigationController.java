package nz.ac.auckland.se283.navigation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import nz.ac.auckland.se283.tools.HardwareClient;

public class NavigationController {
  private final HardwareClient hardwareClient;
  // private final Set<String> validCommands = Set.of("moveForward");
  private final HashMap<String, Integer> validCommands = new HashMap<>();

  public NavigationController(HardwareClient hardwareClient) {
    this.hardwareClient = hardwareClient;
    validCommands.put("moveForward", 1);
    validCommands.put("moveBackward", -1);
    validCommands.put("stop", 2);
  }

  public HashMap<String, Queue<Integer>> processInput(String[] commands) {
    HashMap<String, Queue<Integer>> movementMap = new HashMap<>();
    for (String command : commands) {
      if (validCommands.containsKey(command)) {
        Queue<Integer> tempQueue =
            movementMap.computeIfAbsent("movementQueue", k -> new LinkedList<>());
        tempQueue.add(validCommands.get(command));
      }
    }

    return movementMap;
  }
}
