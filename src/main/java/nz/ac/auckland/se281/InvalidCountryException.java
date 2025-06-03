package nz.ac.auckland.se281;

public class InvalidCountryException extends Exception {
  public InvalidCountryException(String invalidInput) {
    super(MessageCli.INVALID_COUNTRY.getMessage(invalidInput));
  }
}
