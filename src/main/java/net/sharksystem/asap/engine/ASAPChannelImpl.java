package net.sharksystem.asap.engine;

import net.sharksystem.asap.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ASAPChannelImpl implements ASAPChannel {
    private static final String CHANNEL_OWNER = "ASAPChannelOwner";
    private final ASAPEngine asapEngine;
    private final CharSequence uri;

    public ASAPChannelImpl(ASAPEngine asapEngine, CharSequence uri) {
        this.asapEngine = asapEngine;
        this.uri = uri;
    }

    @Override
    public CharSequence getOwner() throws IOException {
        return this.asapEngine.getExtra(this.getUri(), CHANNEL_OWNER);
    }

    @Override
    public CharSequence getUri() throws IOException {
        return this.uri;
    }

    @Override
    public Set<CharSequence> getRecipients() throws IOException {
        return this.asapEngine.getRecipients(this.getUri());
    }

    @Override
    public HashMap<String, String> getExtraData() throws IOException {
        return this.asapEngine.getStorage().getChunk(uri, this.asapEngine.getEra()).getExtraData();
    }

    @Override
    public void putExtraData(String key, String value) throws IOException {
        this.asapEngine.getStorage().getChunk(uri, this.asapEngine.getEra()).putExtra(key, value);
    }

    @Override
    public void removeExtraData(String key) throws IOException {
        this.asapEngine.getStorage().getChunk(uri, this.asapEngine.getEra()).removeExtra(key);
    }

    @Override
    public ASAPMessages getMessages() throws IOException {
        return this.asapEngine.getChunkStorage().getASAPMessages(this.getUri(), this.asapEngine.getEra());
    }

    @Override
    public ASAPMessages getMessages(boolean peerOnly) throws IOException, ASAPException {
        if(peerOnly) return this.getMessages();
        // else
        return this.getMessages(null);
    }

    @Override
    public ASAPMessages getMessages(ASAPMessageCompare compare) throws IOException, ASAPException {
        List<ASAPMessages> messagesSource = new ArrayList<>();

        // add message from local peer first
        messagesSource.add(this.getMessages());

        // other sender?
        List<CharSequence> sender = this.asapEngine.getSender();
        for(CharSequence senderID : sender) {
            try {
                ASAPStorage existingIncomingStorage = this.asapEngine.getExistingIncomingStorage(senderID);
                int currentEra = existingIncomingStorage.getEra();
                // want all messages
                int beforeCurrentEra = ASAP.previousEra(currentEra);
                // currentEra -> direct predecessor
                messagesSource.add(existingIncomingStorage.getChunkStorage().getASAPMessages(
                        this.getUri(), currentEra, beforeCurrentEra));
            } catch (ASAPException e) {
                // no such storage - ok ignore and go ahead
            }
        }

        return new ASAPMessagesMerger(messagesSource, compare);
    }

    @Override
    public void addMessage(byte[] message) throws IOException {
        this.asapEngine.add(this.uri, message);
    }

    public void setOwner(CharSequence owner) throws IOException {
        this.asapEngine.putExtra(this.getUri(), CHANNEL_OWNER, owner.toString());
    }
}
