package net.sharksystem.asap;

import net.sharksystem.asap.internals.ASAPException;
import net.sharksystem.asap.internals.ASAPInternalOnlinePeersChangedListener;
import net.sharksystem.asap.internals.ASAPInternalPeer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ASAPInternalPeerWrapper extends ASAPListenerManagingPeer
        implements ASAPInternalOnlinePeersChangedListener {

    private ASAPInternalPeer peer;

    protected void setInternalPeer(ASAPInternalPeer peer) {
        this.peer = peer;
        this.peer.addOnlinePeersChangedListener(this);

        this.log("activate online messages on that peer");
        this.peer.activateOnlineMessages();
    }

    protected ASAPInternalPeer getInternalPeer() {
        return this.peer;
    }

    public CharSequence getPeerName() {
        return this.peer.getOwner();
    }

    public void handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException {
        this.peer.handleConnection(is, os);
    }

    @Override
    public void onlinePeersChanged(ASAPInternalPeer peer) {
        this.environmentChangesListenerManager.notifyListeners(peer.getOnlinePeers());
    }
}
