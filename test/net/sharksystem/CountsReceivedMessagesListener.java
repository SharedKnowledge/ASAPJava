package net.sharksystem;

import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.ASAPMessageReceivedListener;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.EncounterConnectionType;

import java.io.IOException;

public class CountsReceivedMessagesListener implements ASAPMessageReceivedListener {
    private final String peerName;
    public int numberOfMessages = 0;

    public CountsReceivedMessagesListener(String peerName) {
        this.peerName = peerName;
    }

    CountsReceivedMessagesListener() {
        this(null);
    }

    @Override
    public void asapMessagesReceived(ASAPMessages messages,
                                     String senderE2E, // E2E part
                                     ASAPHop asapHop) throws IOException {
        CharSequence format = messages.getFormat();
        CharSequence uri = messages.getURI();
        if (peerName != null) {
            System.out.print(peerName);
        }

        System.out.println("asap message received (" + format + " | " + uri + "). size == " + messages.size());

        this.numberOfMessages++;
    }
}