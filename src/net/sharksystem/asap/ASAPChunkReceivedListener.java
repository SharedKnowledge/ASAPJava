package net.sharksystem.asap;

/**
 *
 * @author thsc
 */
public interface ASAPChunkReceivedListener {
    void chunkReceived(String sender, String uri, int era);
}
