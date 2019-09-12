package net.sharksystem.asap.protocol;

import java.io.IOException;
import java.io.OutputStream;

public interface ASAPOnlineMessageSource {
    boolean sendMessages(ASAPOnlineConnection asapOnlineConnection, OutputStream os) throws IOException;
}
