package net.sharksystem;

import net.sharksystem.utils.Log;

public abstract class AbstractSharkComponent implements SharkComponent {
    public void setBehaviour(String behaviourName, boolean on) throws SharkUnknownBehaviourException {
        String message = "unknown SharkComponent behaviour: " + behaviourName;
        Log.writeLogErr(this, message);
        throw new SharkUnknownBehaviourException(message);
    }
}
