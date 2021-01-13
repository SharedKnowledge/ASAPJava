package net.sharksystem.asap;

public interface ASAPPeer extends
        ASAPMessageSender,
        ASAPEnvironmentChangesListenerManagement,
        ASAPMessageReceivedListenerManagement
{
    CharSequence getPeerName();
}
