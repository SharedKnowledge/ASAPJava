package net.sharksystem.asap;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * An AASP chunk contains messages regarding a topic described by an
 * uri. Messages are completly opaque to AASP, they are the payload
 * of that protocol. Of course, that protocol was created with ASIP in mind.
 * 
 * Most developers will not work directly with that interface. If so: Take care!
 * 
 * Each chunk has an era. The era number space is a circle with 
 * a definition of proceeding and suceeding era, @see ASAPEngine for details.
 * 
 * Each chunk can have a number of recipients. Note: That list can be 
 * changed by an application as long as that chunk is filled. The ASAPEngine
 * changes that list when meeting other peers. Actually, it removes any peer
 * with whom it got synchronized.
 * 
 * 
 * @author thsc
 */
public interface ASAPChunk {
    /**
     * 
     * @return number of message in that chunk
     */
    int getNumberMessage();
    
    /**
     * URI which represents topic of messages in that chunk
     * @return
     * @throws IOException 
     */
    String getUri() throws IOException;

    /**
     *
     * @return iterator of all messages in the chunk
     * @throws IOException
     */
    Iterator<CharSequence> getMessagesAsCharSequence() throws IOException;

    /**
     *
     * @return iterator of all messages in the chunk
     * @throws IOException
     */
    Iterator<byte[]> getMessages() throws IOException;

    /**
     * remove that chunk.. drop all object references after
     * calling this methods. Further calls on this object
     * have an undefined behaviour.
     */
    public void drop();
    
    public int getEra() throws IOException;
    
    /**
     * 
     * @return recipients of that chunk 
     */
    Set<CharSequence> getRecipients();

    /**
     * add recipients
     * @param recipient
     * @throws IOException 
     */
    void addRecipient(CharSequence recipient) throws IOException;

    /**
     * set a list of recipients. Former recipients are dikscarded
     * @param recipients
     * @throws IOException
     */
    void setRecipients(Collection<CharSequence> recipients) throws IOException;

    /**
     * recipient is removed
     * @param recipients
     * @throws IOException 
     */
    void removeRecipient(CharSequence recipients) throws IOException;

    void addMessage(byte[] messageAsBytes) throws IOException;

    void addMessage(InputStream messageByteIS, long length) throws IOException;

    public long getLength();

    List<Long> getOffsetList();

    InputStream getMessageInputStream();

    void putExtra(String key, String value) throws IOException;

    CharSequence removeExtra(String key) throws IOException;

    CharSequence getExtra(String key) throws IOException;

    /**
     * set up this chunk by a source
     * @param chunkSource
     */
    void clone(ASAPChunk chunkSource) throws IOException;

    HashMap<String, String> getExtraData();

    void deliveredTo(String peer) throws IOException;

    List<CharSequence> getDeliveredTo();

    void copyMetaData(ASAPChannel channel) throws IOException;
}
