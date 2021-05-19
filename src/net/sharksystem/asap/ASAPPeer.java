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
     * ASAP peer are both: application host and potential router. Routing can be switched on or off.
     * We tend to label this behaviour with different ages (stone. bronze, internet), see discussion there.
     * <br/><br/>
     * If true: This engine will route received messages
     * @return
     */
    boolean asapRoutingAllowed(CharSequence applicationFormat) throws IOException, ASAPException;

    void setAsapRoutingAllowed(CharSequence applicationFormat, boolean allowed)
            throws IOException, ASAPException;

    /**
     * Returns true if both peer represent the same peer - ID are compared.
     * @param otherPeer
     * @return
     */
    boolean samePeer(ASAPPeer otherPeer);

    boolean samePeer(CharSequence otherPeerID);
}
