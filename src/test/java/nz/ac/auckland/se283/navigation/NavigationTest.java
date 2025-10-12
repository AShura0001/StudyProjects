package nz.ac.auckland.se283.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import nz.ac.auckland.se283.tools.HardwareClient;
import org.junit.jupiter.api.Test;

public class NavigationTest {
  private HardwareClient hardwareClient;
  private NavigationController navigationController = new NavigationController(hardwareClient);

  @Test
  public void command_moveForward_returnMovementQueue() {
    String[] commands = {"moveForward", "moveForward", "moveForward"};

    HashMap<String, Queue<Integer>> result = navigationController.processInput(commands);

    assertTrue(result.containsKey("movementQueue"));
  }

  @Test
  public void command_moveForward_returnCorrectMovementQueue() {
    String[] commands = {"moveForward", "moveForward", "moveForward"};
    Queue<Integer> expectedQueue = new LinkedList<>(Arrays.asList(1, 1, 1));

    HashMap<String, Queue<Integer>> result = navigationController.processInput(commands);

    assertEquals(expectedQueue, result.get("movementQueue"));
  }

  @Test
  public void command_moveBackward_returnMovementQueue() {
    String[] commands = {"moveBackward"};

    HashMap<String, Queue<Integer>> result = navigationController.processInput(commands);

    assertTrue(result.containsKey("movementQueue"));
  }

  @Test
  public void command_moveBackward_returnCorrectMovementQueue() {
    String[] commands = {"moveBackward", "moveForward", "moveBackward"};
    Queue<Integer> expectedQueue = new LinkedList<>(Arrays.asList(-1, 1, -1));

    HashMap<String, Queue<Integer>> result = navigationController.processInput(commands);

    assertEquals(expectedQueue, result.get("movementQueue"));
  }

  @Test
  public void command_stop_returnMovementQueue() {
    String[] commands = {"stop"};

    HashMap<String, Queue<Integer>> result = navigationController.processInput(commands);

    assertTrue(result.containsKey("movementQueue"));
  }

  @Test
  public void command_stop_returnCorrectMovementQueue() {
    String[] commands = {"moveBackward", "moveForward", "stop", "moveBackward"};
    Queue<Integer> expectedQueue = new LinkedList<>(Arrays.asList(-1, 1, 0, -1));

    HashMap<String, Queue<Integer>> result = navigationController.processInput(commands);

    assertEquals(expectedQueue, result.get("movementQueue"));
  }
}
