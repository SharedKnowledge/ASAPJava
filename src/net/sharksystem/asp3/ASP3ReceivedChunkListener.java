package net.sharksystem.asp3;

/**
 *
 * @author thsc
 */
public interface ASP3ReceivedChunkListener {
    public void chunkReceived(String sender, String Uri, int era);
}
