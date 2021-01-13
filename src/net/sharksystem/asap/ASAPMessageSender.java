package net.sharksystem.asap;

import net.sharksystem.asap.internals.ASAPException;

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
}
