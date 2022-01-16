package net.sharksystem.testsupport;

import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.ASAPMessageReceivedListener;
import net.sharksystem.asap.ASAPMessages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StoreReceivedMessages implements ASAPMessageReceivedListener {
    public List<ASAPMessages> messageList = new ArrayList<>();
    @Override
    public void asapMessagesReceived(ASAPMessages messages, String senderE2E, List<ASAPHop> asapHops) throws IOException {
        this.messageList.add(messages);
    }
}
