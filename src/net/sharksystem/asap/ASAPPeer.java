package net.sharksystem.asap;

import java.io.IOException;

public interface ASAPPeer extends
        ASAPMessageSender,
        ASAPEnvironmentChangesListenerManagement,
        ASAPMessageReceivedListenerManagement
{
    CharSequence UNKNOWN_USER = "anon";
    boolean ONLINE_EXCHANGE_DEFAULT = true;

    CharSequence getPeerID();

    ASAPStorage getASAPStorage(CharSequence format) throws IOException, ASAPException;

    /**
     * Returns true if both peer represent the same peer - ID are compared.
     * @param otherPeer
     * @return
     */
    boolean samePeer(ASAPPeer otherPeer);

    boolean samePeer(CharSequence otherPeerID);
}
