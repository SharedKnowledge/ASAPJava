package net.sharksystem;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.utils.Log;

import java.io.IOException;

public abstract class AbstractSharkComponent implements SharkComponent {
    public void setBehaviour(String behaviourName, boolean on)
            throws SharkUnknownBehaviourException, IOException, ASAPException {
        String message = "unknown SharkComponent behaviour: " + behaviourName;
        Log.writeLogErr(this, message);
        throw new SharkUnknownBehaviourException(message);
    }
}
