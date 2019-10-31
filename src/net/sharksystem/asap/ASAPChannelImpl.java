package net.sharksystem.asap;

import java.io.IOException;
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

    public void setOwner(CharSequence owner) throws IOException {
        this.asapEngine.putExtra(this.getUri(), CHANNEL_OWNER, owner.toString());
    }
}
