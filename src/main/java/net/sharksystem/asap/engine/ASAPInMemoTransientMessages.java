package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAPChunk;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.protocol.ASAP_AssimilationPDU_1_0;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class ASAPInMemoTransientMessages implements ASAPMessages, MessagesContainer {
    private final CharSequence format;
    private final CharSequence uri;
    private final CharSequence sender;
    private int size = -1;
    private List<ASAPHop> asapHopList;
    private List<byte[]> messageList = new ArrayList<>();

    ASAPInMemoTransientMessages(ASAP_AssimilationPDU_1_0 pdu) {
        this.format = pdu.getFormat();
        this.uri = pdu.getChannelUri();
        this.sender = pdu.getSender();
    }

    private void checkStatus() throws ASAPException {
        if(this.size < 0) throw new ASAPException("transient message container not yet filled");
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

    public CharSequence getSender() { return this.sender; }


    @Override
    public void setASAPHopList(List<ASAPHop> asapHopList) throws IOException {
        this.asapHopList = asapHopList;
    }

    @Override
    public Iterator<CharSequence> getMessagesAsCharSequence() throws IOException {
        throw new IOException("not implemented yet");
    }

    @Override
    public Iterator<byte[]> getMessages() throws IOException {
        return this.messageList.iterator();
    }

    @Override
    public CharSequence getMessageAsCharSequence(int position, boolean chronologically) throws ASAPException, IOException {
        return new String(this.getMessage(position, chronologically));
    }

    @Override
    public byte[] getMessage(int position, boolean chronologically) throws ASAPException, IOException {
        if(position > this.messageList.size() || position < 0)
            throw new ASAPException("no message on index (out of range): " + position);

        int index = chronologically ? position : this.messageList.size() - position;

        return this.messageList.get(index);
    }

    @Override
    public ASAPChunk getChunk(int position, boolean chronologically) throws IOException, ASAPException {
        throw new ASAPException("transient message are not stored");
    }

    @Override
    public void addMessage(InputStream is, long length) throws IOException {
        if(length > Integer.MAX_VALUE) throw new IOException("length exceeds range of integer value");

        byte[] message = new byte[(int)length];
        is.read(message);

        this.messageList.add(message);
    }
}
