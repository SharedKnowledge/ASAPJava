package net.sharksystem.asap.engine;

import java.io.IOException;

interface ASAPEnginePermissionSettings {
    /**
     * Engine can remember peers they encountered. It is assumed that those peers are kept with the local peer (not only
     * in this engine)
     * @param on
     * @throws IOException
     */
    void setRememberEncounteredPeers(boolean on) throws IOException;

    /**
     * Engine would ignore all unencrypted messages if set true.
     * @param on
     * @throws IOException
     */
    void setReceivedMessagesMustBeEncrypted(boolean on) throws IOException;

    /**
     * Engine would ignore all unsigned messages if set true.
     * @param on
     * @throws IOException
     */
    void setReceivedMessagesMustBeSigned(boolean on) throws IOException;

    /**
     * Define with what peers an engine is allowed to communicate
     * @param safetyLevel
     * @throws IOException
     */
    void setSetAllowedRemotePeers(AllowedRemotePeers safetyLevel);

    public enum AllowedRemotePeers {
        ANY_PEER /** no restrictions - allowed to communicate with any peer */,
        PEERS_MET /** allowed to communicate with already encountered peers */,
        PEERS_VERIFIED /** allowed to communicate with peers who can be verified with a certificate */;
    }

    /**
     * Engine is allowed to reveal its supported format or not
     * @param peerName can be null for anonymous peer
     * @return
     */
    boolean setRevealEngineFormat(String peerName);

    /**
     * Engine is allowed to send open (messages with no recipient specified) messages to a peer
     * @param peerName can be null for anonymous peer
     * @return
     */
    boolean setSendOpenMessages(String peerName);
}
