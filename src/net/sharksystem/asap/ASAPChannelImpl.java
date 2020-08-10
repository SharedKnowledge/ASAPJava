package net.sharksystem.asap;

import java.io.IOException;
import java.util.HashMap;
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
    public ASAPMessages getMessages() throws IOException {
        return this.asapEngine.getChunkStorage().getASAPMessages(this.getUri(), this.asapEngine.getEra());
    }

    @Override
    public void addMessage(byte[] message) throws IOException {
        this.asapEngine.add(this.uri, message);
    }

    public void setOwner(CharSequence owner) throws IOException {
        this.asapEngine.putExtra(this.getUri(), CHANNEL_OWNER, owner.toString());
    }
}
