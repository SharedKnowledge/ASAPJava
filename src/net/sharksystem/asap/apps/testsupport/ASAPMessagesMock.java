package net.sharksystem.asap.apps.testsupport;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.internals.MessageIter;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

class ASAPMessagesMock implements ASAPMessages {
    private final CharSequence appName;
    private final CharSequence uri;
    private final List<byte[]> serializedAppPDUs;

    ASAPMessagesMock(CharSequence appName, CharSequence uri, List<byte[]> serializedAppPDUs) {
        this.appName = appName;
        this.uri = uri;
        this.serializedAppPDUs = serializedAppPDUs;
    }

    @Override
    public int size() throws IOException {
        return this.serializedAppPDUs.size();
    }

    @Override
    public CharSequence getURI() {
        return this.uri;
    }

    @Override
    public CharSequence getFormat() {
        return this.appName;
    }

    @Override
    public Iterator<CharSequence> getMessagesAsCharSequence() throws IOException {
        return new MessageIter(this.serializedAppPDUs);
    }

    @Override
    public Iterator<byte[]> getMessages() throws IOException {
        return this.serializedAppPDUs.iterator();
    }

    @Override
    public CharSequence getMessageAsCharSequence(int position, boolean chronologically) throws ASAPException, IOException {
        throw new ASAPException("not implemented in mocking class");
    }

    @Override
    public byte[] getMessage(int position, boolean chronologically) throws ASAPException, IOException {
        throw new ASAPException("not implemented in mocking class");
    }
}
