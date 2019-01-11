package net.sharksystem.asp3;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * A chunk represents a set of messages addressed by an URI. 
 *  
 * 
 * @author thsc
 * @see ASPChunkStorage for more details
 */
public interface ASP3Chunk2Send extends ASP3Chunk {
    /**
     * 
     * @return recipients of that chunk 
     */
    List<CharSequence> getRecipients();

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
    void setRecipients(List<CharSequence> recipients) throws IOException;

    /**
     * recipient is removed
     * @param recipients
     * @throws IOException 
     */
    void removeRecipient(CharSequence recipients) throws IOException;

    /**
     * adds a message
     * @param message
     * @throws IOException 
     */
    void add(CharSequence message) throws IOException;
}
