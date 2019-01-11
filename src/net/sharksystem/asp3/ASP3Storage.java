package net.sharksystem.asp3;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author thsc
 */
public interface ASP3Storage {

    public ASP3Chunk2Send getChunk(CharSequence urlTarget, int era) throws IOException;

    public List<ASP3Chunk2Send> getChunks(int era) throws IOException;

    public void dropChunks(int era) throws IOException;
}
