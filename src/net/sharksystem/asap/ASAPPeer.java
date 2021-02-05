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
}
