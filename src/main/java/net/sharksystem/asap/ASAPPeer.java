package net.sharksystem.asap;

import java.io.IOException;

public interface ASAPPeer extends
        ASAPMessageSender,
        ASAPEnvironmentChangesListenerManagement,
        ASAPMessageReceivedListenerManagement,
        ASAPChannelContentChangedListenerManagement
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
    /*
    ASAPConnection handleConnection(InputStream is, OutputStream os, boolean encrypt, boolean sign,
                                    EncounterConnectionType connectionType) throws IOException, ASAPException;


     */

    /**
     * Make a value persistent with key
     * @param key
     * @param value
     */
    void putExtra(CharSequence key, byte[] value) throws IOException, ASAPException;

    /**
     * Return a value. Throws an exception if not set
     * @param key
     * @throws ASAPException key never used in putExtra
     */
    byte[] getExtra(CharSequence key) throws ASAPException, IOException;
}
