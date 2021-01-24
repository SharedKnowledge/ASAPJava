package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.protocol.ASAPConnection;
import net.sharksystem.asap.protocol.ASAPOnlineMessageSource;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

public class ASAPSingleProcessOnlineMessageSender
        extends ASAPAbstractOnlineMessageSender implements ASAPOnlineMessageSource {

    private final ASAPOnlineMessageSenderEngineSide asapOnlineMessageSenderEngineSide;

    public ASAPSingleProcessOnlineMessageSender(ASAPInternalPeer multiEngine, ASAPInternalStorage source) {
        this.attachToSource(source);
        this.asapOnlineMessageSenderEngineSide = new ASAPOnlineMessageSenderEngineSide(multiEngine);
    }

    @Override
    public void sendASAPAssimilateMessage(CharSequence format, CharSequence uri, Set<CharSequence> recipients,
                                          byte[] messageAsBytes, int era) throws IOException, ASAPException {

        this.asapOnlineMessageSenderEngineSide.sendASAPAssimilateMessage(
                format, uri, recipients, messageAsBytes, era);
    }

    public void sendASAPAssimilateMessage(CharSequence format, CharSequence uri, byte[] messageAsBytes)
            throws IOException, ASAPException {
        this.sendASAPAssimilateMessage(format, uri, messageAsBytes, ASAPEngineFS.DEFAULT_INIT_ERA);
    }

    @Override
    public void sendASAPAssimilateMessage(CharSequence format, CharSequence uri, byte[] messageAsBytes, int era)
            throws IOException, ASAPException {

        this.asapOnlineMessageSenderEngineSide.sendASAPAssimilateMessage(format, uri, messageAsBytes, era);
    }

    @Override
    public void sendStoredMessages(ASAPConnection asapConnection, OutputStream os) throws IOException {
        this.asapOnlineMessageSenderEngineSide.sendStoredMessages(asapConnection, os);
    }
}
