package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAPChannel;
import net.sharksystem.asap.ASAPChunk;
import net.sharksystem.asap.ASAPHop;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * An ASAP chunk is a set of message with the same format, same uri and same era.
 *
 * Reading from a chunk can be done anytime.
 *
 * Use change methods only if you really know what you do. Most likely, there will be
 * a running ASAP protocol in the background that writes data into chunks, namely chunks
 * of current era. The protocol engine reads also from older era to transmit messages to
 * encountered peers. There will be a documentation that explains strategies when to do what.
 *
 * Rule of thumb: Read is ok. Better do not change anything with this interface.
 *
 * @author thsc
 */
public interface ASAPInternalChunk extends ASAPChunk {
    /**
     * Convenient methode: It calls getMessages and transforms each message into a string
     * @return iterator of all messages in the chunk
     * @throws IOException
     */
    Iterator<CharSequence> getMessagesAsCharSequence() throws IOException;

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

    void addMessage(InputStream messageByteIS, long length) throws IOException;

    long getLength();

    List<Long> getOffsetList();

    InputStream getMessageInputStream();

    void putExtra(String key, String value) throws IOException;

    CharSequence removeExtra(String key) throws IOException;

    CharSequence getExtra(String key) throws IOException;

    /**
     * set up this chunk by a source
     * @param chunkSource
     */
    void clone(ASAPInternalChunk chunkSource) throws IOException;

    HashMap<String, String> getExtraData();

    void deliveredTo(String peer) throws IOException;

    List<CharSequence> getDeliveredTo();

    void copyMetaData(ASAPChannel channel) throws IOException;
}
