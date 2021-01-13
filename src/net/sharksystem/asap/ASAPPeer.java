package net.sharksystem.asap;

public interface ASAPPeer extends
        ASAPMessageSender,
        ASAPEnvironmentChangesListenerManagement,
        ASAPMessageReceivedListenerManagement
{
    CharSequence UNKNOWN_USER = "anon";
    boolean ONLINE_EXCHANGE_DEFAULT = true;

    CharSequence getPeerName();
}
