package lavs.ui.service;

/** Thrown when a screen asks for something the layer below cannot do yet; the message is for the user. */
public class NotSupportedYetException extends RuntimeException {

    public NotSupportedYetException(String message) {
        super(message);
    }
}
