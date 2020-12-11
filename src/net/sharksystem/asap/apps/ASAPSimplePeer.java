package net.sharksystem.asap.apps;

public interface ASAPSimplePeer extends
        ASAPMessageSender,
        ASAPEnvironmentChangesListenerManagement,
        ASAPMessageReceivedListenerManagement
{
    CharSequence getPeerName();
}
