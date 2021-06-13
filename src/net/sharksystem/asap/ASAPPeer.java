package net.sharksystem.asap;

import net.sharksystem.asap.protocol.ASAPConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
    boolean isASAPRoutingAllowed(CharSequence applicationFormat) throws IOException, ASAPException;

    void setASAPRoutingAllowed(CharSequence applicationFormat, boolean allowed)
            throws IOException, ASAPException;

    /**
     * Returns true if both peer represent the same peer - ID are compared.
     * @param otherPeer
     * @return
     */
    boolean samePeer(ASAPPeer otherPeer);

    boolean samePeer(CharSequence otherPeerID);

    /**
     * Handle a connection
     * @param is
     * @param os
     * @param encrypt point-to-point encryption
     * @param sign point-to-point signing / verifying ?
     * @param connectionType
     * @return
     * @throws IOException
     * @throws ASAPException
     */
    ASAPConnection handleConnection(InputStream is, OutputStream os, boolean encrypt, boolean sign,
                                    EncounterConnectionType connectionType) throws IOException, ASAPException;
}
