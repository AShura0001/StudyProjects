package nz.ac.auckland.se281;

public class InvalidCountryException extends Exception {
  public InvalidCountryException() {
    super("This is not a valid country, try different input!");
  }
}
