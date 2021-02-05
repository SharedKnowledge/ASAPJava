package net.sharksystem.asap.engine;

import net.sharksystem.asap.*;
import net.sharksystem.asap.management.ASAPManagementStorage;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 *
 * Break down of a communication channel in ad-hoc networks is normal and barely failure.
 * That chunk storage is meant to keep messages which are produced by an
 * app for later transmission.
 * 
 * Messages which cannot be sent to their recipients can be stored in ASAP chunks.
 * Each chunk is addressed with an URI (comparable to URIs e.g. in Android
 * Content Provider)
 * 
 * Applications can easily store their messages by calling add(URI, message).
 * That message is stored in a chunk addressed by the URI. 
 * 
 * Each chunk has a recipient list which can be changed anytime. The ASAPEngine
 * uses those information for sending such stored messages whenever a peer
 * establishes a connection.
 * 
 * It is recommended to use ASAPEngineFS to set up that framework.
 * Create a ASAPEngine like this
 * 
 * <pre>
 * AASPReader reader = ...;
 * ASAPStorage myStorage = ASAPEngineFS.getASAPEngine("EngineName", "ChunkStorageRootFolder", reader);
 * </pre>
 * 
 * An ASAPReader must be implemented prior using that framework. Objects of
 * that class are called whenever another peer transmits messages to the
 * local peer. @see AASPReader
 * 
 * Chunks are structured by eras. In most cases, application developers don't 
 * have to care about era management at all. If so, take care. Eras are usually 
 * changed by the ASAPEngine whenever a peer (re-) connects. In that case, the
 * current era is declared to be finished and an new era is opened. 
 * Any new message is now tagged as message from that new era. The ASAPEngine
 * transmits all message to the peer which are stored after the final
 * encounter. If no encounter ever happened - all available messages are
 * transmitted. 
 *
 * @see ASAPEngine
 *
 * @author thsc
 */
public interface ASAPInternalStorage extends ASAPStorage {
    /**
     * Creates a channel with named recipients - we call it a closed channel in opposite
     * to an open channel.
     *
     * Peers must not forward messages from a closed to other peers than those in recipient list.
     *
     * @param uri
     * @param recipients
     * @throws IOException
     */
    void createChannel(CharSequence uri, Collection<CharSequence> recipients) throws IOException, ASAPException;

    /**
     * Create channel (owner can differ from local peer owing this asap engine)
     * @param owner
     * @param uri
     * @param recipients
     * @throws IOException
     * @throws ASAPException
     */
    void createChannel(CharSequence owner, CharSequence uri, Collection<CharSequence> recipients) throws IOException, ASAPException;

    /**
     * Create a channel with only two members - creator and recipient
     * @param urlTarget
     * @param recipient
     * @throws IOException
     */
    void createChannel(CharSequence urlTarget, CharSequence recipient) throws IOException, ASAPException;

    /**
     * Create open channel
     * @param urlTarget
     * @throws IOException
     * @throws ASAPException
     */
    void createChannel(CharSequence urlTarget) throws IOException, ASAPException;

    /**
     * Chunks are delivered when seeing other peers. This flag allows to decide whether delivered chunks
     * are to be deleted.
     * @param drop
     */
//    void setDropDeliveredChunks(boolean drop) throws IOException;

    /**
     * Chunks are delivered when seeing other peers. Default behaviour is to send only message which
     * are in local peers own storage. A peer can also have received messages in an incoming storage.
     * This flag allows to force even delivery of received messages from incoming storages. Basis of
     * multihop communication.
     *
     * @param drop
     */
//    void setSendReceivedChunks(boolean drop) throws IOException;

    /**
     /**
     * returns recipient list
     *
     * @param urlTarget chunk address
     * @throws IOException
     * @return
     */
    Set<CharSequence> getRecipients(CharSequence urlTarget) throws IOException;

    void addRecipient(CharSequence urlTarget, CharSequence recipient) throws IOException;
    void removeRecipient(CharSequence urlTarget, CharSequence recipient) throws IOException;

    /**
     * Add a message to that chunk.
     * @param uri message topic
     * @param message Message to be kept for later transmission
     * @throws IOException 
     */
    void add(CharSequence uri, CharSequence message) throws IOException;

    void attachASAPMessageAddListener(ASAPOnlineMessageSender asapOnlineMessageSender);

    void detachASAPMessageAddListener();

    void setASAPManagementStorage(ASAPManagementStorage asapManagementStorage);

    boolean isASAPManagementStorageSet();

    /**
     * Create a new era
     */
    public void newEra();
    
    /**
     * Default behaviour of ASAPEngine: Each peer / communication partner
     * gets its own chunk storage. That storage is filled during asap
     * synchronization. That storage can be retrieved with this command.
     * 
     * @param sender
     * @return 
     */
    ASAPChunkStorage getReceivedChunksStorage(CharSequence sender);

    ASAPMessages getChunkChain(int uriPosition) throws IOException, ASAPException;

    ASAPMessages getChunkChain(int uriPosition, int toEra) throws IOException, ASAPException;

    ASAPMessages getChunkChain(CharSequence uri, int toEra) throws IOException;

    ASAPMessages getChunkChain(CharSequence uri) throws IOException;

    /**
     * Refresh with external system - re-read files, or whatever.
     * @return refreshed object
     */
    ASAPInternalStorage refresh() throws IOException, ASAPException;
}
