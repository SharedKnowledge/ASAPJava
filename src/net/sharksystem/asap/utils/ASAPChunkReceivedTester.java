package net.sharksystem.asap.utils;

import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.engine.ASAPChunkReceivedListener;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author thsc
 */
public class ASAPChunkReceivedTester implements ASAPChunkReceivedListener {
    private String senderE2E = null;
    private String uri = null;
    private int era;
    private String format;

    @Override
    public void chunkReceived(String format, String senderE2E, String uri, int era,
                              List<ASAPHop> asapHop) throws IOException {

        System.out.println("ChunkReceiverTester.chunkReceived called: (format|sender|uri|era) " +
                format +
                " | " +
                senderE2E +
                " | " +
                uri +
                " | " +
                era);
        this.format = format;
        this.senderE2E = senderE2E;
        this.uri = uri;
        this.era = era;
    }

    public boolean chunkReceived() {
        return this.senderE2E != null;
    }

    public String getSenderE2E() {
        return this.senderE2E;
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
