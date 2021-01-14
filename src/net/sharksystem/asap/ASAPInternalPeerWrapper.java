package net.sharksystem.asap;

import net.sharksystem.asap.internals.ASAPInternalOnlinePeersChangedListener;
import net.sharksystem.asap.internals.ASAPInternalPeer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

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
    public void notifyOnlinePeersChanged(ASAPInternalPeer peer) {
        this.notifyOnlinePeersChanged(peer.getOnlinePeers());
    }

    public void notifyOnlinePeersChanged(Set<CharSequence> peerList) {
        StringBuilder sb = new StringBuilder();
        sb.append("#online peers: ");
        sb.append(peerList.size());
        for(CharSequence peerName : peerList) {
            sb.append(" | ");
            sb.append(peerName);
        }

        this.log(sb.toString());
        this.environmentChangesListenerManager.notifyListeners(peerList);
    }
}
