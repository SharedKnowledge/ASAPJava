package net.sharksystem.asap;

import java.io.IOException;

public interface ASAPMessageReceivedListener {
    void asapMessagesReceived(ASAPMessages messages, String senderE2E, // E2E part
                              ASAPHop asapHop /* Point-to-point part */ ) throws IOException;
}