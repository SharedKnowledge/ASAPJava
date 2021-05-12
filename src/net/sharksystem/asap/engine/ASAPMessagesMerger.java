package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessageCompare;
import net.sharksystem.asap.ASAPMessages;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class ASAPMessagesMerger implements ASAPMessages {
    private final List<ASAPMessages> messageSources;
    private final ASAPMessageCompare messageCompare;

    ASAPMessagesMerger(List<ASAPMessages> messageSources, ASAPMessageCompare messageCompare) throws ASAPException {
        if(messageSources == null || messageSources.isEmpty())
            throw new ASAPException("message source must not be null or empty");

        this.messageSources = messageSources;
        this.messageCompare = messageCompare;
    }

    @Override
    public int size() throws IOException {
        return 0;
    }

    @Override
    public CharSequence getURI() {
        return null;
    }

    @Override
    public CharSequence getFormat() {
        return null;
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
