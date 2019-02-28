package net.sharksystem.aasp.util;

import net.sharksystem.aasp.AASPReceivedChunkListener;

/**
 *
 * @author thsc
 */
public class AASPChunkReceiverTester implements AASPReceivedChunkListener {
    private String sender = null;
    private String uri = null;
    private int era;

    @Override
    public void chunkReceived(String sender, String uri, int era) {
        System.out.println("ChunkReceiverTester.chunkReceived called: (sender/uri/era) " + 
                sender +
                " / " + 
                uri +
                " / " + 
                era);
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

    public String getUri() {
        return this.uri;
    }

    public int getEra() {
        return this.era;
    }
}
