package net.sharksystem.aasp;

import java.io.IOException;
import java.util.Iterator;

/**
 * Chunks are identified by an URI and ordered by era numbers.
 * User applications can often define an URI but would hardly like
 * to deal with era management. 
 * 
 * This interface helps those applications. Classes implementing that
 * interface wrap an actual chunk storage and offer a view of an ordered
 * list of message. Especially era management is hidden with this interfaces.
 * 
 * It is meant to be a cache. It is *not* assumed that this cache is
 * always in sync with the actual chunk storage. Syncing is up to application
 * which makes implementing that cache easier.
 * 
 * 
 * @author thsc
 */
public interface AASPChunkCache {
    /**
     * 
     * @return number of messages fitting to that uri regardless of era
     */
    public int getNumberMessage() throws IOException;
    
    /**
     * 
     * @return uri of that chunk cache
     */
    public CharSequence getURI();
    
    /**
     * 
     * first, false: newest message comes first
     * @return iterator of all messages in that chunk cache
     * @throws IOException 
     */
    Iterator<CharSequence> getMessages() throws IOException;
    
    /**
     * Returns a message with a given position
     * @param position
     * @param chronologically in chronological order: true: oldest message comes
     * first, false: newest message comes first
     * @return message
     * @throws net.sharksystem.aasp.AASPException message on that position does
     * not exist
     * @throws IOException couldn't read from storage
     */
    CharSequence getMessage(int position, boolean chronologically) 
            throws AASPException, IOException;
    
    /**
     * Add a message. Implementations are free to simply delegate that method to 
     * the underlying chunk storage. They can also decide to keep the cache in
     * sync. That could be useful if only one application uses the chunk storage.
     * It's up to implementation and using application to clarify semantics
     * of this method.
     * 
     * @param message
     * @throws IOException 
     */
    void add(CharSequence message) throws IOException;

}
