package net.sharksystem;

import net.sharksystem.asap.ASAPPeer;

public class YourComponentImpl implements SharkComponent {
    @Override
    public void onStart(ASAPPeer peer) throws SharkException {
        System.out.println("component started");
        // do something useful
    }

    @Override
    public void setBehaviour(String behaviourName, boolean on) throws SharkUnknownBehaviourException {
        throw new SharkUnknownBehaviourException("unknown behaviour: " + behaviourName);
    }
}
