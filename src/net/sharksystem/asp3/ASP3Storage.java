package net.sharksystem.asp3;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author thsc
 */
interface ASP3Storage {

    ASP3Chunk getChunk(CharSequence urlTarget, int era) throws IOException;

    List<ASP3Chunk> getChunks(int era) throws IOException;
    
    void dropChunks(int era) throws IOException;
}
