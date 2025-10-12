package nz.ac.auckland.se283.navigation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Queue;
import nz.ac.auckland.se283.tools.HardwareClient;
import org.junit.jupiter.api.Test;

public class NavigationTest {
  private HardwareClient hardwareClient;

  @Test
  public void command_moveForward_returnMovementQueue() {
    String[] commands = {"moveForward", "moveForward", "moveForward"};
    NavigationController navigationController = new NavigationController(hardwareClient);
    HashMap<String, Queue<Integer>> result = navigationController.processInput(commands);
    assertTrue(result.containsKey("movementQueue"));
  }
}
