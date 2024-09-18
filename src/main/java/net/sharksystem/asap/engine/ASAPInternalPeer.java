package net.sharksystem.asap.engine;

import net.sharksystem.SharkException;
import net.sharksystem.asap.ASAPConnectionHandler;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.protocol.*;
import net.sharksystem.asap.crypto.ASAPKeyStore;
import net.sharksystem.fs.ExtraData;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

/**
 * There is an ASAPEngine that stores its data with a filesystem.
 * One significant parameter is a root directory.
 *
 * It is good practice to use a different root for each application.
 *
 * It is also common that more than one ASAP based app is running
 * on one machine. Thus, different ASAP filesystem based engine are
 * to deal with the data depending on the ASAP format.
 *
 * That interface hides those different engines.
 */
public interface ASAPInternalPeer extends ASAPConnectionHandler, ExtraData {
    long DEFAULT_MAX_PROCESSING_TIME = Long.MAX_VALUE;

    /**
     * get an existing engine
     * @param format
     * @return
     * @throws ASAPException engine does not exist
     * @throws IOException
     */
    ASAPEngine getEngineByFormat(CharSequence format) throws ASAPException, IOException;

    boolean asapRoutingAllowed(CharSequence applicationFormat) throws IOException, ASAPException;

    void setAsapRoutingAllowed(CharSequence applicationFormat, boolean allowed)
            throws IOException, ASAPException;

    /**
     * return already existing or create an engine for a given format / application name
     * @param format
     * @return
     * @throws ASAPException
     * @throws IOException
     */
    ASAPEngine createEngineByFormat(CharSequence format) throws ASAPException, IOException;

    ASAPChunkAssimilatedListener getListenerByFormat(CharSequence format) throws ASAPException;

    /**
     * get or create engine for a given application - mainly means: setup folder
     * @param format
     * @return
     */
    ASAPEngine getASAPEngine(CharSequence format) throws IOException, ASAPException;

    void pushInterests(OutputStream os) throws IOException, ASAPException;

    Set<CharSequence> getOnlinePeers();

    boolean existASAPConnection(CharSequence recipient);

    ASAPConnection getASAPConnection(CharSequence recipient);

    CharSequence getOwner();

    void newEra() throws IOException, ASAPException;

    void setASAPChunkReceivedListener(CharSequence appName, ASAPChunkAssimilatedListener listener) throws ASAPException;

    void addOnlinePeersChangedListener(ASAPInternalOnlinePeersChangedListener listener);

    void removeOnlinePeersChangedListener(ASAPInternalOnlinePeersChangedListener listener);

    /**
     * @return true if the asap management engine is up and running
     */
    boolean isASAPManagementEngineRunning();

    EngineSetting getEngineSettings(CharSequence format) throws ASAPException;

    /**
     * @return all formats currently supported by this mulit engine
     */
    Set<CharSequence> getFormats();

    void activateOnlineMessages();
    void deactivateOnlineMessages();

    /**
     * A transient message is not stored and not meant to be forwarded. Sending a transient message has no effect
     * without a running encounter. Despite that, it is an ordinary ASAP message - described by an application/format
     * and an optional uri.
     *
     * @param nextHopPeerIDs A peer can have multiple encounter at the same time. This list - if present, names
*                       potential message receiver. If null, message is sent to any open connection. An exception
     *                            is <b>not thrown</b> if there is no connection to one or more peers in the list
     * @param format message application / format
     * @param urlTarget describe message within your app
     * @param messageAsBytes serialized message
     * @throws IOException
     * @throws ASAPException
     */
    void sendTransientASAPAssimilateMessage(CharSequence format, CharSequence urlTarget,
                                            Set<CharSequence> nextHopPeerIDs, byte[] messageAsBytes) throws IOException, ASAPException;

    /**
     * Send a transient message to a single peer.
     * <b>An exception is thrown if there is no open connection the the specified peer</b>
     * @param format
     * @param urlTarget
     * @param nextHopPeerID
     * @param messageAsBytes
     * @throws IOException
     * @throws ASAPException
     */
    void sendTransientASAPAssimilateMessage(CharSequence format, CharSequence urlTarget,
                                            CharSequence nextHopPeerID, byte[] messageAsBytes) throws IOException, ASAPException;

    /**
     * Send a transient message to any peer we have an open connection to.
     *
     * @param format
     * @param urlTarget
     * @param messageAsBytes
     * @throws IOException
     * @throws ASAPException
     */
    void sendTransientASAPAssimilateMessage(CharSequence format, CharSequence urlTarget, byte[] messageAsBytes)
            throws IOException, ASAPException;

    /**
     * @deprecated use sendTransientASAPAssimilateMessage instead
     * @param format
     * @param urlTarget
     * @param era
     * @param recipients
     * @param messageAsBytes
     * @throws IOException
     * @throws ASAPException
     */
    void sendOnlineASAPAssimilateMessage(CharSequence format, CharSequence urlTarget, int era,
         Set<CharSequence> recipients, byte[] messageAsBytes) throws IOException, ASAPException;

    void sendOnlineASAPAssimilateMessage(CharSequence format, CharSequence urlTarget, int era, byte[] messageAsBytes)
            throws IOException, ASAPException;

    void setASAPKeyStore(ASAPKeyStore ASAPKeyStore);
    ASAPKeyStore getASAPKeyStore();

    ASAPCommunicationSetting getASAPCommunicationControl();

    ASAPKeyStore getAsapKeyStore() throws ASAPSecurityException;

    ExtraData getExtraData() throws SharkException, IOException;

    void setSecurityAdministrator(DefaultSecurityAdministrator securityAdministrator);

}
