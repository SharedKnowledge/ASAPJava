package net.sharksystem.asap.util;

import net.sharksystem.asap.ASAPChunkReceivedListener;

/**
 *
 * @author thsc
 */
public class ASAPChunkReceivedTester implements ASAPChunkReceivedListener {
    private String sender = null;
    private String uri = null;
    private int era;
    private String format;

    @Override
    public void chunkReceived(String format, String sender, String uri, int era) {
        System.out.println("ChunkReceiverTester.chunkReceived called: (format|sender|uri|era) " +
                format +
                " | " +
                sender +
                " | " +
                uri +
                " | " +
                era);
        this.format = format;
        this.sender = sender;
        this.uri = uri;
        this.era = era;
    }

    public boolean chunkReceived() {
        return this.sender != null;
    }

    public String getSender() {
        return this.sender;
    }

    public String getFormat() {
        return this.format;
    }

    public String getUri() {
        return this.uri;
    }

    public int getEra() {
        return this.era;
    }
}
