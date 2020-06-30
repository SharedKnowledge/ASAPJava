package net.sharksystem.asap;

import java.io.IOException;

/**
 *
 * @author thsc
 */
public interface ASAPChunkReceivedListener {
    /**
     * @param format
     * @param sender
     * @param uri
     * @param era
     */
    void chunkReceived(String format, String sender, String uri, int era) throws IOException;
}
