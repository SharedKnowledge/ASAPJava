package net.sharksystem.asap;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.engine.ASAPInternalChunk;

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
public interface ASAPMessages {
    /**
     * @return number of messages
     */
    int size() throws IOException;

    /**
     * @return channel uri
     */
    CharSequence getURI();

    /**
     * @return format which - in most cases - matches with an application name
     */
    CharSequence getFormat();

    /**
     *
     * first, false: newest message comes first
     * @return iterator of all messages in that chunk cache
     * @throws IOException
     * @deprecated this lib works supports byte[] - apps deal with (de)serialization.
     */
    Iterator<CharSequence> getMessagesAsCharSequence() throws IOException;

    /**
     *
     * first, false: newest message comes first
     * @return iterator of all messages in that chunk cache
     * @throws IOException
     */
    Iterator<byte[]> getMessages() throws IOException;

    /**
     * Returns a message with a given position
     * @param position
     * @param chronologically in chronological order: true: oldest message comes
     * first, false: newest message comes first
     * @return message
     * @throws ASAPException message on that position does
     * not exist
     * @throws IOException couldn't read from storage
     */
    CharSequence getMessageAsCharSequence(int position, boolean chronologically)
            throws ASAPException, IOException;

    byte[] getMessage(int position, boolean chronologically)
            throws ASAPException, IOException;

    /**
     * Return chunk in which message at position is to be found
     * @param position
     * @param chronologically
     * @return
     */
    ASAPChunk getChunk(int position, boolean chronologically) throws IOException, ASAPException;
}
