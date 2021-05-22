package net.sharksystem.components;

import net.sharksystem.SharkException;
import net.sharksystem.SharkUnknownBehaviourException;
import net.sharksystem.asap.*;
import net.sharksystem.asap.utils.ASAPMessages2StringCollectionWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class YourComponentImpl implements YourComponent, ASAPMessageReceivedListener {
    private List<YourComponentListener> listenerList = new ArrayList<>();
    private ASAPPeer asapPeer;

    @Override
    public void onStart(ASAPPeer peer) throws SharkException {
        System.out.println("component started");
        // do something useful

        // remember this peer
        this.asapPeer = peer;

        // listen to message type A
        this.asapPeer.addASAPMessageReceivedListener(
                YourComponent.FORMAT_A, this);

        // listen to message type B - could also implement a different listener object
        this.asapPeer.addASAPMessageReceivedListener(
                YourComponent.FORMAT_B, this);


    }

    @Override
    public void setBehaviour(String behaviourName, boolean on) throws SharkUnknownBehaviourException {
        throw new SharkUnknownBehaviourException("unknown behaviour: " + behaviourName);
    }

    @Override
    public void subscribeYourComponentListener(YourComponentListener listener) {
        this.listenerList.add(listener);
    }

    @Override
    public void sendBroadcastMessageA(String uri, String broadcast) {
        // serialize your structure - simple here.
        byte[] messageBytes = this.serializeMessage(broadcast);
        try {
            this.asapPeer.sendASAPMessage(YourComponent.FORMAT_A, uri, messageBytes);
        } catch (ASAPException e) {
            System.err.println("cannot send message: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Iterator<String> getMessagesA(String uri) throws IOException, ASAPException {
        ASAPStorage asapStorage = this.asapPeer.getASAPStorage(YourComponent.FORMAT_A);
        ASAPChannel channel = asapStorage.getChannel(uri);
        ASAPMessages messages = channel.getMessages();

        return new ASAPMessages2StringCollectionWrapper(messages);
    }

    String deserializeMessage(byte[] messageBytes) {
        // deserialize - convert into your structured data - just a string in this example
        return new String(messageBytes);
    }

    byte[] serializeMessage(String message) {
        // deserialize - convert into your structured data - just a string in this example
        return message.getBytes();
    }

    @Override
    public void asapMessagesReceived(ASAPMessages asapMessages,
                                     String senderE2E, // E2E part
                                     List<ASAPHop> asapHop) throws IOException {
        try {
            // get first message
            byte[] message = asapMessages.getMessage(0, true);

            // deserialize byte[] into your message structure
            String messageString = this.deserializeMessage(message);
            CharSequence format = asapMessages.getFormat();

            if(format.toString().equalsIgnoreCase(YourComponent.FORMAT_A)) {
                for(YourComponentListener l : this.listenerList) {
                    l.somethingHappenedFormatA(messageString);
                }
            }
            else if(format.toString().equalsIgnoreCase(YourComponent.FORMAT_B)) {
                for(YourComponentListener l : this.listenerList) {
                    l.somethingHappenedFormatB(messageString);
                }
            }
            else {
                System.err.println("fatal: received message with unknown format: " + format);
            }

        } catch (ASAPException e) {
            e.printStackTrace();
        }
    }
}
