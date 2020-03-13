package net.sharksystem.asap.protocol;

import java.io.IOException;
import java.io.OutputStream;

public interface ASAPOnlineMessageSource {
    void sendStoredMessages(ASAPConnection asapConnection, OutputStream os) throws IOException;
}
