package net.sharksystem.aasp;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author thsc
 */
public interface ASP3Storage {

    public ASP3Chunk getChunk(CharSequence urlTarget, int era) throws IOException;

    public List<ASP3Chunk> getChunks(int era) throws IOException;

    public void dropChunks(int era) throws IOException;
}
