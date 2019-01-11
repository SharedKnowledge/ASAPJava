package net.sharksystem.asp3;

import java.io.IOException;

/**
 *
 * @author thsc
 */
public interface ASP3ChunkReceived extends ASP3Chunk {
    CharSequence getSender() throws IOException;
    
    void addReceivedMessage(CharSequence message) throws IOException;
}
