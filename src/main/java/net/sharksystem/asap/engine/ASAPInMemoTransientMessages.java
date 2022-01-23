package net.sharksystem.asap.engine;

import net.sharksystem.asap.*;
import net.sharksystem.asap.protocol.ASAP_AssimilationPDU_1_0;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ASAPInMemoTransientMessages implements ASAPMessages, MessagesContainer {
    private final CharSequence format;
    private final CharSequence uri;
    private final CharSequence sender;
    private ASAPHop asapHop;
    private int size = -1;
    private List<byte[]> messageList = new ArrayList<>();

    ASAPInMemoTransientMessages(ASAP_AssimilationPDU_1_0 pdu, ASAPHop asapHop) {
        this.format = pdu.getFormat();
        this.uri = pdu.getChannelUri();
        this.sender = pdu.getSender();
        this.asapHop = asapHop;
    }

    public ASAPInMemoTransientMessages(CharSequence format, CharSequence uri, CharSequence sender, ASAPHop asapHop) {
        this.format = format;
        this.uri = uri;
        this.sender = sender;
        this.asapHop = asapHop;
    }

    private void checkStatus() throws IOException {
        if(this.size < 0) throw new IOException("transient message container not yet filled");
    }

    @Override
    public int size() throws IOException {
        this.checkStatus();
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
        if(this.asapHop == null) {
            this.asapHop = asapHopList.get(asapHopList.size()-1);
        }
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

        this.addMessage(message);
    }

    public void addMessage(byte[] message) throws IOException {
        this.messageList.add(message);
    }
}
