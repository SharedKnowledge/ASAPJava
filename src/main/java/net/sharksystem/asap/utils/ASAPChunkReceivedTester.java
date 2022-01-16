package net.sharksystem.asap.utils;

import net.sharksystem.asap.ASAP;
import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.engine.ASAPChunkAssimilatedListener;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author thsc
 */
public class ASAPChunkReceivedTester implements ASAPChunkAssimilatedListener {
    private CharSequence senderE2E = null;
    private CharSequence uri = null;
    private int era;
    private CharSequence format;

    @Override
    public void chunkStored(String format, String senderE2E, String uri, int era,
                            List<ASAPHop> asapHop) throws IOException {

        this.chunkAssimilated(format, senderE2E, uri, era, asapHop);
    }

    @Override
    public void transientChunkReceived(ASAPMessages transientMessages, CharSequence sender, List<ASAPHop> asapHop) throws IOException {
        this.chunkAssimilated(transientMessages.getFormat(), sender, transientMessages.getURI(), ASAP.TRANSIENT_ERA, asapHop);
    }

    private void chunkAssimilated(CharSequence format, CharSequence senderE2E, CharSequence uri, int era, List<ASAPHop> asapHop) {
        System.out.println("chunk assimilated called: (format|sender|uri|era) " +
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
        return this.senderE2E.toString();
    }

    public String getFormat() {
        return this.format.toString();
    }

    public String getUri() {
        return this.uri.toString();
    }

    public int getEra() {
        return this.era;
    }
}
