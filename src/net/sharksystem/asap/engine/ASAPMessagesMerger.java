package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessageCompare;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.utils.PeerIDHelper;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class ASAPMessagesMerger implements ASAPMessages {
    private final List<ASAPMessages> messageSources;
    private final ASAPMessageCompare messageCompare;
    private CharSequence format;
    private CharSequence uri;
    private int size;

    ASAPMessagesMerger(List<ASAPMessages> messageSources, ASAPMessageCompare messageCompare)
            throws ASAPException, IOException {
        if(messageSources == null || messageSources.isEmpty())
            throw new ASAPException("message source must not be null or empty");

        this.messageSources = messageSources;
        this.messageCompare = messageCompare;
        this.size = 0; // init;

        // check integrity
        this.format = null;
        this.uri = null;
        for(ASAPMessages source : messageSources) {
            CharSequence currentFormat = source.getFormat();
            if(this.format != null && !PeerIDHelper.sameFormat(currentFormat, this.format)) {
                throw new ASAPException("message source must not have different formats: " + this.format
                        + " | " + currentFormat);
            }
            // else
            this.format = currentFormat;

            CharSequence currentUri = source.getURI();
            if(this.uri != null && !PeerIDHelper.sameFormat(currentUri, this.uri)) {
                throw new ASAPException("message source must not have different uris: " + this.uri
                        + " | " + currentUri);
            }
            // else
            this.uri = currentUri;

            this.size += source.size();
        }
    }

    @Override
    public int size() throws IOException {
        return this.size;
    }

    @Override
    public CharSequence getURI() {
        return this.uri;
    }

    @Override
    public CharSequence getFormat() {
        return this.format;
    }

    @Override
    public Iterator<CharSequence> getMessagesAsCharSequence() throws IOException {
        return null;
    }

    @Override
    public Iterator<byte[]> getMessages() throws IOException {
        return null;
    }

    @Override
    public CharSequence getMessageAsCharSequence(int position, boolean chronologically) throws ASAPException, IOException {
        return null;
    }

    @Override
    public byte[] getMessage(int position, boolean chronologically) throws ASAPException, IOException {
        return new byte[0];
    }
}
