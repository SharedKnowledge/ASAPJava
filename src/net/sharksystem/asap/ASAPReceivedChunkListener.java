package net.sharksystem.asap;

/**
 *
 * @author thsc
 */
public interface ASAPReceivedChunkListener {
    public void chunkReceived(String sender, String Uri, int era);
}
