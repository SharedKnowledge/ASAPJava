package net.sharksystem.asap;

import java.io.IOException;

public interface ASAPMessageSender {
    /**
     * Send a message
     * @param appName
     * @param uri
     * @param message
     * @throws ASAPException
     */
    void sendASAPMessage(CharSequence appName, CharSequence uri,
                         byte[] message) throws ASAPException;

    /**
     * When calling this methode, this asap message is sent over any existing connection.
     * It is not stored on sender or receiver side. Message listeners are called as usual. Nothing happens (no
     * exception is thrown) if there is not a single peer encounter running.
     * @param appName
     * @param uri
     * @param message
     * @throws ASAPException
     * @throws IOException
     */
    void sendTransientASAPMessage(CharSequence appName, CharSequence uri, byte[] message)
            throws ASAPException, IOException;

    /**
     * When calling this methode, this asap message is sent over any existing connection.
     * It is not stored on sender or receiver side. Message listeners are called as usual. Nothing happens (no
     * exception is thrown) if there is not a single peer encounter running.
     *
     * @param nextHopPeerID Peer will try to send a message only to this peer. An ASAPException is thrown if there
*                      is no running encounter with that peer in place.
     * @param appName
     * @param uri
     * @param message
     * @throws ASAPException
     * @throws IOException
     */
    void sendTransientASAPMessage(CharSequence nextHopPeerID, CharSequence appName, CharSequence uri, byte[] message)
            throws ASAPException, IOException;
}
