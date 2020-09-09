package net.sharksystem.asap.sharknet;

import java.util.ArrayList;
import java.util.List;

public class SharkNetMessageReceivedListener implements SharkNetMessageListener {
    public List<SharkNetMessage> receivedMessages = new ArrayList<>();

    @Override
    public void messageReceived(byte[] message, CharSequence topic, CharSequence senderID, boolean verified, boolean encrypted) {
        this.receivedMessages.add(new SharkNetMessage(message, topic, senderID, verified, encrypted));
    }
}
