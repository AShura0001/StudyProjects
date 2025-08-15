package nz.ac.auckland.se206;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

import javafx.scene.input.MouseEvent;
import nz.ac.auckland.se206.states.GameOver;
import nz.ac.auckland.se206.states.GameStarted;
import nz.ac.auckland.se206.states.GameState;
import nz.ac.auckland.se206.states.Guessing;

/**
 * Context class for managing the state of the game. Handles transitions between different game
 * states and maintains game data such as the professions and rectangle IDs.
 */
public class GameStateContext {

  private final String rectIdToGuess;
  private final String professionToGuess;
  private final Map<String, String> rectanglesToProfession;
  private final GameStarted gameStartedState;
  private final Guessing guessingState;
  private final GameOver gameOverState;
  private GameState gameState;

  /** Constructs a new GameStateContext and initializes the game states and professions. */
  public GameStateContext() {
    gameStartedState = new GameStarted(this);
    guessingState = new Guessing(this);
    gameOverState = new GameOver(this);

    gameState = gameStartedState; // Initial state
    Map<String, Object> obj = null;
    Yaml yaml = new Yaml();
    try (InputStream inputStream =
        GameStateContext.class.getClassLoader().getResourceAsStream("data/professions.yaml")) {
      if (inputStream == null) {
        throw new IllegalStateException("File not found!");
      }
      obj = yaml.load(inputStream);
    } catch (IOException e) {
      e.printStackTrace();
    }

    @SuppressWarnings("unchecked")
    List<String> professions = (List<String>) obj.get("professions");

    Random random = new Random();
    Set<String> randomProfessions = new HashSet<>();
    while (randomProfessions.size() < 3) {
      String profession = professions.get(random.nextInt(professions.size()));
      randomProfessions.add(profession);
    }

    String[] randomProfessionsArray = randomProfessions.toArray(new String[3]);
    rectanglesToProfession = new HashMap<>();
    rectanglesToProfession.put("rectPerson1", randomProfessionsArray[0]);
    rectanglesToProfession.put("rectPerson2", randomProfessionsArray[1]);
    rectanglesToProfession.put("rectPerson3", randomProfessionsArray[2]);

    int randomNumber = random.nextInt(3);
    rectIdToGuess =
        randomNumber == 0 ? "rectPerson1" : ((randomNumber == 1) ? "rectPerson2" :
  "rectPerson3");
    professionToGuess = rectanglesToProfession.get(rectIdToGuess);
  }

  /**
   * Sets the current state of the game.
   *
   * @param state the new state to set
   */
  public void setState(GameState state) {
    this.gameState = state;
  }

  /**
   * Gets the initial game started state.
   *
   * @return the game started state
   */
  public GameState getGameStartedState() {
    return gameStartedState;
  }

  /**
   * Gets the guessing state.
   *
   * @return the guessing state
   */
  public GameState getGuessingState() {
    return guessingState;
  }

  /**
   * Gets the game over state.
   *
   * @return the game over state
   */
  public GameState getGameOverState() {
    return gameOverState;
  }

  /**
   * Gets the profession to be guessed.
   *
   * @return the profession to guess
   */
  public String getProfessionToGuess() {
    return professionToGuess;
  }

  /**
   * Gets the ID of the rectangle to be guessed.
   *
   * @return the rectangle ID to guess
   */
  public String getRectIdToGuess() {
    return rectIdToGuess;
  }

  /**
   * Gets the profession associated with a specific rectangle ID.
   *
   * @param rectangleId the rectangle ID
   * @return the profession associated with the rectangle ID
   */
  public String getProfession(String rectangleId) {
    return rectanglesToProfession.get(rectangleId);
  }

  /**
   * Handles the event when a rectangle is clicked.
   *
   * @param event the mouse event triggered by clicking a rectangle
   * @param rectangleId the ID of the clicked rectangle
   * @throws IOException if there is an I/O error
   */
  public void handleRectangleClick(MouseEvent event, String rectangleId) throws IOException {
    gameState.handleRectangleClick(event, rectangleId);
  }

  /**
   * Handles the event when the guess button is clicked.
   *
   * @throws IOException if there is an I/O error
   */
  public void handleGuessClick() throws IOException {
    gameState.handleGuessClick();
  }

  // // Working
  // private final GameStarted gameStartedState;
  // private final Guessing guessingState;
  // private final GameOver gameOverState;
  // private GameState gameState;

  // private final Set<String> rectangleIds;

  // /** Constructs a new GameStateContext with fixed rectangle IDs. */
  // public GameStateContext() {
  //   gameStartedState = new GameStarted(this);
  //   guessingState = new Guessing(this);
  //   gameOverState = new GameOver(this);

  //   gameState = gameStartedState;

  //   // Fixed rectangle IDs
  //   rectangleIds =
  //       new HashSet<>(Arrays.asList("aiDefendantRect", "aiWitnessRect", "humanWitnessRect"));
  // }

  // public void setState(GameState state) {
  //   this.gameState = state;
  // }

  // public GameState getGameStartedState() {
  //   return gameStartedState;
  // }

  // public GameState getGuessingState() {
  //   return guessingState;
  // }

  // public GameState getGameOverState() {
  //   return gameOverState;
  // }

  // // public String getRole(String rectangleId) {}

  // /**
  //  * Handles a click on a rectangle, delegating to the current game state's logic.
  //  *
  //  * @param event the mouse click event
  //  * @param rectangleId the ID of the rectangle that was clicked
  //  * @throws IOException if an error occurs during event handling
  //  */
  // public void handleRectangleClick(MouseEvent event, String rectangleId) throws IOException {
  //   if (rectangleIds.contains(rectangleId)) {
  //     gameState.handleRectangleClick(event, rectangleId);
  //   } else {
  //     System.out.println("Invalid rectangle ID clicked: " + rectangleId);
  //   }
  // }

  // /**
  //  * Handles a click on the guess/next button — still useful for transitioning between states.
  //  *
  //  * @throws IOException if an error occurs during event handling
  //  */
  // public void handleGuessClick() throws IOException {
  //   gameState.handleGuessClick();
  // }
}
