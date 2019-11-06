package net.sharksystem.asap;

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
    void chunkReceived(String format, String sender, String uri, int era);
}
