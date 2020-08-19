package net.sharksystem.asap;

import net.sharksystem.asap.management.ASAPManagementStorage;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
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
public interface ASAPStorage {
    /**
     * Get owner name (better an id) of this storage
     * @return owner name (or id)
     */
    CharSequence getOwner();

    /**
     *
     * @return storage format / app
     */
    CharSequence getFormat();

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
     *
     * @return list of channel uris present in this storage
     * @throws IOException
     */
    List<CharSequence> getChannelURIs() throws IOException;

    /**
     *
     * @param uri
     * @return channel containing all message issued by owner of this storage. Received messages are <b>not</b> part
     * of this channel. (See makan implementation)
     * @throws ASAPException if no channel with that uri exists in this storage
     */
    ASAPChannel getChannel(CharSequence uri) throws ASAPException, IOException;

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

    void removeChannel(CharSequence uri) throws IOException;

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
     * Put some extra information on that channel
     * @param uri describing the channel
     * @param key
     * @param value
     * @throws IOException
     */
    void putExtra(CharSequence uri, String key, String value) throws IOException;

    /**
     * Remove a string from extra information set
     * @param uri
     * @param key
     * @return
     * @throws IOException
     */
    CharSequence removeExtra(CharSequence uri, String key) throws IOException;

    /**
     * Get a string from extra information set without removing
     * @param uri
     * @param key
     * @return
     * @throws IOException
     */
    CharSequence getExtra(CharSequence uri, String key) throws IOException;

    /**
     *
     * @param uri channel uri
     * @return if channel exists - in other words: at least one message was added with this channel uri
     */
    boolean channelExists(CharSequence uri) throws IOException;
    
    /**
     * Add a message to that chunk.
     * @param uri message topic
     * @param message Message to be kept for later transmission
     * @throws IOException 
     */
    void add(CharSequence uri, CharSequence message) throws IOException;

    void add(CharSequence uri, byte[] messageAsBytes) throws IOException;

    void attachASAPMessageAddListener(ASAPOnlineMessageSender asapOnlineMessageSender);

    void detachASAPMessageAddListener();

    void setASAPManagementStorage(ASAPManagementStorage asapManagementStorage);

    boolean isASAPManagementStorageSet();

    /**
     * Create a new era
     */
    public void newEra();
    
    /**
     * Get oldest era available on that peer.
     * @return 
     */
    public int getOldestEra();
    
    /**
     * Get current era.
     * @return 
     */
    public int getEra();
    
    /**
     * Get next era number. Era numbers are organized in a circle. Number 0
     * follows Integer.MAXVALUE. That method takes care of that fact.
     * No change is made on current era.
     * 
     * @param era
     * @return 
     */
    public int getNextEra(int era);
    
    /**
     * Get previous era number. Era numbers are organized in a circle. Er 
     * number 0 is proceeded by era number Integer.MAXVALUE. 
     * That method takes care of that fact.
     * No change is made on current era.
     * 
     * @param era
     * @return 
     */
    int getPreviousEra(int era);
    
    /**
     * Default behaviour of ASAPEngine: Each peer / communication partner
     * gets its own chunk storage. That storage is filled during asap
     * synchronization. That storage can be retrieved with this command.
     * 
     * @param sender
     * @return 
     */
    ASAPChunkStorage getReceivedChunksStorage(CharSequence sender);

    ASAPStorage getExistingIncomingStorage(CharSequence sender) throws IOException, ASAPException;

    /**
     *
     * @return list of peers with an incoming chunk storage
     */
    List<CharSequence> getSender();

    /**
     * 
     * @return The local chunk storage that is meant to be used by the local
     * app. Note: That storage is changed during an ASAP session.
     */
    ASAPChunkStorage getChunkStorage();

    ASAPMessages getChunkChain(int uriPosition) throws IOException, ASAPException;

    ASAPMessages getChunkChain(int uriPosition, int toEra) throws IOException, ASAPException;

    ASAPMessages getChunkChain(CharSequence uri, int toEra) throws IOException;

    ASAPMessages getChunkChain(CharSequence uri) throws IOException;

    /**
     * Refresh with external system - re-read files, or whatever.
     * @return refreshed object
     */
    ASAPStorage refresh() throws IOException, ASAPException;
}
