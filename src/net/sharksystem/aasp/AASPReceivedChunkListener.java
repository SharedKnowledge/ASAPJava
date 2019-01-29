package net.sharksystem.aasp;

/**
 *
 * @author thsc
 */
public interface AASPReceivedChunkListener {
    public void chunkReceived(String sender, String Uri, int era);
}
