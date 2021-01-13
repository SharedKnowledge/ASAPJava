package net.sharksystem.asap.protocol;

import net.sharksystem.asap.internals.ASAPException;

public class ASAPExecTimeExceededException extends ASAPException {
    public ASAPExecTimeExceededException() {
        super();
    }

    public ASAPExecTimeExceededException(String message) {
        super(message);
    }
}
