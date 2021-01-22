package net.sharksystem.asap;

import java.io.IOException;
import java.util.List;

public interface ASAPStorage {
    /**
     * Get owner id/name of this storage. It is the name of the local peer.
     * @return owner id/name
     */
    CharSequence getOwner();

    /**
     * @return list of peers with an incoming storage
     * @see #getExistingIncomingStorage(CharSequence)
     */
    List<CharSequence> getSender();

    /**
     * Peer exchange message during an encounter. Received messages are stored in
     * an <i>incoming storage</i>
     * @param sender
     * @return storage with received messages from a peer
     * @throws IOException
     * @throws ASAPException
     */
    ASAPStorage getExistingIncomingStorage(CharSequence sender) throws IOException, ASAPException;

    /**
     *
     * @return storage format / app
     */
    CharSequence getFormat();

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
     *
     * @param uri channel uri
     * @return if channel exists - in other words: at least one message was added with this channel uri
     */
    boolean channelExists(CharSequence uri) throws IOException;

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
     *
     * @return The local chunk storage that is meant to be used by the local
     * app. Note: That storage is changed during an ASAP session.
     */
    ASAPChunkStorage getChunkStorage();

    void createChannel(CharSequence urlTarget) throws IOException, ASAPException;

    void removeChannel(CharSequence uri) throws IOException;
    /**
     *
     * @param uri
     * @param messageAsBytes
     * @throws IOException
     * @deprecated
     * @see ASAPChannel#addMessage(byte[])
     */
    void add(CharSequence uri, byte[] messageAsBytes) throws IOException;
}
