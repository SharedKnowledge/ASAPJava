package net.sharksystem.asap.mockAndTemplates;

import net.sharksystem.asap.internals.ASAPMessages;
import net.sharksystem.asap.ASAPMessageReceivedListener;

import java.io.IOException;
import java.util.Iterator;

class ASAPMessageReceivedListenerExample implements ASAPMessageReceivedListener {
    private final String peerName;

    ASAPMessageReceivedListenerExample(String peerName) {
        this.peerName = peerName;
    }

    ASAPMessageReceivedListenerExample() {
        this(null);
    }

    @Override
    public void asapMessagesReceived(ASAPMessages messages) throws IOException {
        CharSequence format = messages.getFormat();
        CharSequence uri = messages.getURI();
        if (peerName != null) {
            System.out.print(peerName);
        }

        System.out.println("asap message received (" + format + " | " + uri + "). size == " + messages.size());
        Iterator<byte[]> yourPDUIter = messages.getMessages();
        while (yourPDUIter.hasNext()) {
            TestUtils.deserializeExample(yourPDUIter.next());
        }
    }
}
