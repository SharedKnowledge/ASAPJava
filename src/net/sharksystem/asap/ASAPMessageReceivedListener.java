package net.sharksystem.asap;

import java.io.IOException;

public interface ASAPMessageReceivedListener {
    void asapMessagesReceived(ASAPMessages messages) throws IOException;
}