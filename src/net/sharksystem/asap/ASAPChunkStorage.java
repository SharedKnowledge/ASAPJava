package net.sharksystem.asap;

import net.sharksystem.asap.engine.ASAPInternalChunk;

import java.io.IOException;
import java.util.List;

/**
 * There is a chunk storage for each format. It offers methods
 * to get (and create) and remove (drop) chunks.
 *
 * Be careful. In most cases, there will be an ASAP protocol engine that writes data
 * in chunks and reads and transmits messages to encountered peers.
 *
 * @author thsc
 */
public interface ASAPChunkStorage {
    String getFormat();

    ASAPInternalChunk getChunk(CharSequence uri, int era) throws IOException;

    boolean existsChunk(CharSequence uri, int era) throws IOException;

    List<ASAPInternalChunk> getChunks(int era) throws IOException;

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
