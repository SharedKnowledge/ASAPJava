package net.sharksystem.asap;

import java.io.IOException;
import java.util.List;

public interface ASAPMessageReceivedListener {
    void asapMessagesReceived(ASAPMessages messages, String senderE2E, // E2E part
                              List<ASAPHop> asapHops /* Point-to-point part */ ) throws IOException;
}