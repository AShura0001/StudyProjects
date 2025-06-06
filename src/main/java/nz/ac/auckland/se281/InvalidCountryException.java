package nz.ac.auckland.se281;

public class InvalidCountryException extends Exception {
  public InvalidCountryException(String invalidInput) {
    // defines the exception message using super and the message from MessageCli
    // "ERROR! This country was not found: %s, try again!" which takes the invalidInput
    // as a parameter.
    super(MessageCli.INVALID_COUNTRY.getMessage(invalidInput));
  }
}
