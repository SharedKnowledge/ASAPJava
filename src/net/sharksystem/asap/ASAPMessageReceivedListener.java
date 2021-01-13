package net.sharksystem.asap;

import net.sharksystem.asap.internals.ASAPMessages;

import java.io.IOException;

public interface ASAPMessageReceivedListener {
    void asapMessagesReceived(ASAPMessages messages) throws IOException;
}