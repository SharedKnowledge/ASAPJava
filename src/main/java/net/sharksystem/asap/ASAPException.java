package net.sharksystem.asap;

import net.sharksystem.SharkException;

/**
 *
 * @author thsc
 */
public class ASAPException extends SharkException {
    public ASAPException() { super(); }
    public ASAPException(String message) {
        super(message);
    }
    public ASAPException(String message, Throwable cause) {
        super(message, cause);
    }
    public ASAPException(Throwable cause) {
        super(cause);
    }
}
