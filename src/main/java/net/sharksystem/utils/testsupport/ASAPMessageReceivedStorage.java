package net.sharksystem.utils.testsupport;

import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.ASAPMessageReceivedListener;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ASAPMessageReceivedStorage implements ASAPMessageReceivedListener {
    public List<byte[]> messageStorage = new ArrayList<>();
    @Override
    public void asapMessagesReceived(ASAPMessages messages, String senderE2E, List<ASAPHop> asapHops) throws IOException {
        Log.writeLog(this, "messages received");
        if(messages != null) {
            Iterator<byte[]> messagesIter = messages.getMessages();
            while(messagesIter.hasNext()) {
                this.messageStorage.add(messagesIter.next());
            }
        }
    }

    public int getNumberReceivedMessages() {
        return this.messageStorage.size();
    }
}
