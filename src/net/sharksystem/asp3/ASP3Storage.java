package net.sharksystem.asp3;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author thsc
 */
interface ASP3Storage {

    public ASP3Chunk getChunk(CharSequence urlTarget, int era) throws IOException;

    public List<ASP3Chunk> getChunks(int era) throws IOException;
    
    public void dropChunks(int era) throws IOException;
    
    
}
