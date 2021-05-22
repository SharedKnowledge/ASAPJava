package net.sharksystem;

import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.ASAPMessageReceivedListener;
import net.sharksystem.asap.ASAPMessages;

import java.io.IOException;
import java.util.List;

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
                                     List<ASAPHop> asapHopsList) throws IOException {
        CharSequence format = messages.getFormat();
        CharSequence uri = messages.getURI();
        if (peerName != null) {
            System.out.print(peerName);
        }

        StringBuilder sb = new StringBuilder();
        for(ASAPHop hop : asapHopsList) {
            sb.append(hop.toString());
            sb.append("\n");
        }

        System.out.println("\n###############################################################################");
        System.out.println("messages received (" + format + " | " + uri + "). size == " + messages.size()
                + "hops\n" + sb.toString());
        System.out.println("###############################################################################");

        this.numberOfMessages++;
    }
}