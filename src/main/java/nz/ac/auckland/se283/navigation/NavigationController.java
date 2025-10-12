package nz.ac.auckland.se283.navigation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import nz.ac.auckland.se283.Exceptions.UnsupportedCommandException;
import nz.ac.auckland.se283.tools.HardwareClient;

public class NavigationController {
  private final HardwareClient hardwareClient;
  private HashMap<String, Queue<Integer>> navigationMap = new HashMap<>();
  // private final Set<String> validCommands = Set.of("moveForward");
  // movementQueue - 1, directionQueue - 2
  private final HashMap<String, List<Integer>> validCommands = new HashMap<>();

  public NavigationController(HardwareClient hardwareClient) {
    this.hardwareClient = hardwareClient;
    validCommands.put("moveForward", List.of(1, 1));
    validCommands.put("moveBackward", List.of(-1, 1));
    validCommands.put("stop", List.of(0, 1));
    validCommands.put("turnRight", List.of(1, 2));
    validCommands.put("turnLeft", List.of(2, 2));
  }

  public HashMap<String, Queue<Integer>> processInput(String[] commands)
      throws UnsupportedCommandException {
    for (String command : commands) {
      if (validCommands.containsKey(command)) {
        if (validCommands.get(command).get(1) == 1) {
          Queue<Integer> tempQueue =
              navigationMap.computeIfAbsent("movementQueue", k -> new LinkedList<>());
          tempQueue.add(validCommands.get(command).get(0));
        } else if (validCommands.get(command).get(1) == 2) {
          Queue<Integer> tempQueue =
              navigationMap.computeIfAbsent("directionQueue", k -> new LinkedList<>());
          tempQueue.add(validCommands.get(command).get(0));
        }
      } else {
        throw new UnsupportedCommandException("Unsupported command: " + command);
      }
    }

    return navigationMap;
  }
}
