package net.sharksystem.asap;

import java.io.IOException;
import java.util.List;

/**
 * A storage is a logical unit containing chunks. It offers methods
 * to get (and create) and remove (drop) chunks. There is at least
 * one storage containing message produced by a local app. 
 * 
 * There can be other storages containg messages which arrived from a
 * peer during sychronization.
 * 
 * @author thsc
 */
public interface ASAPChunkStorage {
    String getFormat();

    ASAPChunk getChunk(CharSequence uri, int era) throws IOException;

    boolean existsChunk(CharSequence uri, int era) throws IOException;

    List<ASAPChunk> getChunks(int era) throws IOException;

    void dropChunks(int era) throws IOException;
    
    /**
     * 
     * @param uri chunk storage uri
     * @param toEra newest era
     * @return a chunk cache which hides details of era
     * @throws IOException 
     */
    ASAPMessages getASAPMessages(CharSequence uri, int toEra) throws IOException;

    ASAPMessages getASAPMessages(CharSequence uri, int fromEra, int toEra) throws IOException;

    ASAPMessages getASAPMessages(String uri) throws ASAPException, IOException;
}
