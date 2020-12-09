package net.sharksystem.asap.apps.mock;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.apps.ASAPPeerServices;

public class ASAPSimplePeer extends ASAPBasicAbstractPeer implements ASAPPeerServices {

    protected ASAPSimplePeer(CharSequence peerName) {
        super(peerName);
    }

    @Override
    public void sendASAPMessage(CharSequence appName, CharSequence uri, byte[] message) throws ASAPException {

    }
}
