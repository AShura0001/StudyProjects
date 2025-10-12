package nz.ac.auckland.se283.Exceptions;

public class UnsupportedCommandException extends RuntimeException {
  public UnsupportedCommandException(String message) {
    super(message);
  }
}
