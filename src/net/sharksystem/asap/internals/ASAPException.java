package net.sharksystem.asap.internals;

/**
 *
 * @author thsc
 */
public class ASAPException extends Exception {
    
    public ASAPException() {
        super();
    }
    
    public ASAPException(String message) {
        super(message);
    }

    public ASAPException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
