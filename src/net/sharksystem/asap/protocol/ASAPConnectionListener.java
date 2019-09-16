package net.sharksystem.asap.protocol;

public interface ASAPConnectionListener {
    /**
     * Called when first message was read from remote peer.
     * The session started.
     * @param peer
     */
    void asapConnectionStarted(String peer, ASAPConnection connection);

    void asapConnectionTerminated(Exception terminatingException, ASAPConnection connection);
}
