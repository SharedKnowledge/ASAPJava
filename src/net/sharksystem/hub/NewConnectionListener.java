package net.sharksystem.hub;

/**
 * Peer can establish a connected mediated by a hub. This listener interface is used whenever a new connection
 * was established.
 */
public interface NewConnectionListener {
    /**
     * A new connection was established
     * @param peerConnection i/o streams and further information of the newly established communication
     */
    void notifyPeerConnected(PeerConnection peerConnection);
}
