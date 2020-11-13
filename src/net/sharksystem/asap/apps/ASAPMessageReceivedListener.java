package net.sharksystem.asap.apps;

import net.sharksystem.asap.ASAPMessages;

import java.io.IOException;

public interface ASAPMessageReceivedListener {
    void asapMessagesReceived(ASAPMessages messages) throws IOException;
}