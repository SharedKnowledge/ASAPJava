package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAPConnectionHandler;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.protocol.*;
import net.sharksystem.asap.crypto.ASAPKeyStore;
import net.sharksystem.utils.ExtraData;

import java.io.FileNotFoundException;
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

    ASAPChunkReceivedListener getListenerByFormat(CharSequence format) throws ASAPException;

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

    void setASAPChunkReceivedListener(CharSequence appName, ASAPChunkReceivedListener listener) throws ASAPException;

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
     * This message is only transmitted with open connections. Message is not stored. Nothing happens if no open
     * connection is present.
     * @param format
     * @param urlTarget
     * @param recipients
     * @param messageAsBytes
     * @throws IOException
     * @throws ASAPException
     */
    void sendOnlineASAPAssimilateMessage(CharSequence format, CharSequence urlTarget, Set<CharSequence> recipients,
                                         byte[] messageAsBytes) throws IOException, ASAPException;

    void sendOnlineASAPAssimilateMessage(CharSequence format, CharSequence urlTarget, byte[] messageAsBytes)
            throws IOException, ASAPException;

    void setASAPBasicKeyStorage(ASAPKeyStore ASAPKeyStore);

    ASAPCommunicationSetting getASAPCommunicationControl();

    ASAPKeyStore getASAPKeyStore() throws ASAPSecurityException;

    void setSecurityAdministrator(DefaultSecurityAdministrator securityAdministrator);
}
