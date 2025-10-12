package nz.ac.auckland.se283.navigation;

import java.util.HashMap;
import java.util.Queue;
import nz.ac.auckland.se283.tools.HardwareClient;

public class NavigationController {
  private final HardwareClient hardwareClient;

  public NavigationController(HardwareClient hardwareClient) {
    this.hardwareClient = hardwareClient;
  }

  public HashMap<String, Queue<Integer>> processInput(String[] commands) {
    HashMap<String, Queue<Integer>> movementQueue = new HashMap<>();

    return movementQueue;
  }
}
