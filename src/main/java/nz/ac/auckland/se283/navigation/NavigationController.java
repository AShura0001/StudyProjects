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
  // movementQueue - 1, directionQueue - 2, speedQueue - 3
  private final HashMap<String, List<Integer>> validCommands = new HashMap<>();

  public NavigationController(HardwareClient hardwareClient) {
    this.hardwareClient = hardwareClient;
    validCommands.put("moveForward", List.of(1, 1));
    validCommands.put("moveBackward", List.of(1, -1));
    validCommands.put("stop", List.of(1, 0));
    validCommands.put("turnRight", List.of(2, 1));
    validCommands.put("turnLeft", List.of(2, -1));
    validCommands.put("moveForwardFast", List.of(3, 1, 2));
    validCommands.put("moveBackwardFast", List.of(3, -1, 2));
    validCommands.put("moveBackwardSlow", List.of(3, -1, -1));
    validCommands.put("moveForwardSlow", List.of(3, 1, -1));
  }

  public HashMap<String, Queue<Integer>> processInput(String[] commands)
      throws UnsupportedCommandException {
    for (String command : commands) {
      if (validCommands.containsKey(command)) {
        if (validCommands.get(command).get(0) == 1) {
          Queue<Integer> tempMovementQueue =
              navigationMap.computeIfAbsent("movementQueue", k -> new LinkedList<>());
          tempMovementQueue.add(validCommands.get(command).get(1));
          Queue<Integer> tempSpeedQueue =
              navigationMap.computeIfAbsent("speedQueue", k -> new LinkedList<>());
          if (validCommands.get(command).get(1) == 0) {
            tempSpeedQueue.add(0); // Speed is 0 for stop command
          } else {
            tempSpeedQueue.add(1); // Default speed for moveForward, moveBackward, stop
          }
          Queue<Integer> tempDirectionQueue =
              navigationMap.computeIfAbsent("directionQueue", k -> new LinkedList<>());
          tempDirectionQueue.add(0); // Direction is 0 for movement commands
        } else if (validCommands.get(command).get(0) == 2) {
          Queue<Integer> tempDirectionQueue =
              navigationMap.computeIfAbsent("directionQueue", k -> new LinkedList<>());
          tempDirectionQueue.add(validCommands.get(command).get(1));
          Queue<Integer> tempSpeedQueue =
              navigationMap.computeIfAbsent("speedQueue", k -> new LinkedList<>());
          tempSpeedQueue.add(0); // Default speed for turn commands
          Queue<Integer> tempMovementQueue =
              navigationMap.computeIfAbsent("movementQueue", k -> new LinkedList<>());
          tempMovementQueue.add(0); // Movement is 0 for turn commands
        } else if (validCommands.get(command).get(0) == 3) {
          Queue<Integer> tempMovementQueue =
              navigationMap.computeIfAbsent("movementQueue", k -> new LinkedList<>());
          tempMovementQueue.add(validCommands.get(command).get(1));
          Queue<Integer> tempSpeedQueue =
              navigationMap.computeIfAbsent("speedQueue", k -> new LinkedList<>());
          tempSpeedQueue.add(validCommands.get(command).get(2));
          Queue<Integer> tempDirectionQueue =
              navigationMap.computeIfAbsent("directionQueue", k -> new LinkedList<>());
          tempDirectionQueue.add(0); // Direction is 0 for movement commands
        }
      } else {
        throw new UnsupportedCommandException("Unsupported command: " + command);
      }
    }

    return navigationMap;
  }
}
