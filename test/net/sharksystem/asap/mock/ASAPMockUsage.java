package net.sharksystem.asap.mock;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.apps.ASAPMessageSender;
import net.sharksystem.asap.apps.ASAPMessageReceivedListener;
import net.sharksystem.asap.apps.mock.ASAPSessionMock;
import org.junit.Test;

import java.io.*;
import java.util.Iterator;

/**
 * How to mock ASAP communication
 */
public class ASAPMockUsage {

    private static final CharSequence YOUR_APP_NAME = "yourAppName";
    private static final CharSequence YOUR_URI = "yourSchema://example";

    /**
     * a serialization example
     * @param exampleLong
     * @param exampleString
     * @param exampleBoolean
     * @return
     */
    private static byte[] serializeExample(long exampleLong, String exampleString, boolean exampleBoolean) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream daos = new DataOutputStream(baos);

        // serialize
        daos.writeLong(exampleLong);
        daos.writeUTF(exampleString);
        daos.writeBoolean(exampleBoolean);

        return baos.toByteArray();
    }

    /**
     * a deserialization example
     */
    private static void deserializeExample(byte[] serializedData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
        DataInputStream dais = new DataInputStream(bais);

        // deserialize
        long exampleLong = dais.readLong();
        String exampleString = dais.readUTF();
        boolean exampleBoolean = dais.readBoolean();

        // call a methode in your app

        // here: just print
        System.out.println("received: " + exampleLong + " | " + exampleString + " | " + exampleBoolean);
    }

    @Test
    public void usageTest1() throws IOException, ASAPException, InterruptedException {
        /* Imagine we are here inside your application code. Data are to be transmitted. You implemented
        a methode that serializes your data (PDU) into an array of bytes
         */

        // example - this should be produced by your application
        byte[] serializedData = ASAPMockUsage.serializeExample(42, "don't panic", true);

        // now: ASAP is used to deliver those data - we mock it
        ASAPSessionMock asapSessionMock = new ASAPSessionMock();

        ASAPMessageSender asapMessageSender = asapSessionMock;

        asapMessageSender.sendASAPMessage(YOUR_APP_NAME, YOUR_URI, serializedData);

        // we simulated a sender - now, we need to simulate recipient

        // this should be replaced with your code - you must implement a listener.
        ASAPMessageReceivedListenerExample asapMessageReceivedListenerExample =
                new ASAPMessageReceivedListenerExample();

        // register your listener (or that mock) with asap connection mock
        asapSessionMock.addASAPMessageReceivedListener(YOUR_APP_NAME, asapMessageReceivedListenerExample);

        // simulate ASAP encounter
        asapSessionMock.connect();

        // give your app a moment to process
        Thread.sleep(1000);

        // add another message while still connected
        asapMessageSender.sendASAPMessage(YOUR_APP_NAME, YOUR_URI,
                ASAPMockUsage.serializeExample(43, "second message", false));

        asapSessionMock.disconnect();
        System.out.println("send message without connection");
        asapMessageSender.sendASAPMessage(YOUR_APP_NAME, YOUR_URI,
                ASAPMockUsage.serializeExample(44, "third message", false));
        asapMessageSender.sendASAPMessage(YOUR_APP_NAME, YOUR_URI,
                ASAPMockUsage.serializeExample(45, "forth message", false));
        Thread.sleep(1000);

        System.out.println("re-connect");
        asapSessionMock.connect();
        Thread.sleep(1000);
    }

    private class ASAPMessageReceivedListenerExample implements ASAPMessageReceivedListener {
        @Override
        public void asapMessagesReceived(ASAPMessages messages) throws IOException {
            CharSequence format = messages.getFormat();
            CharSequence uri = messages.getURI();
            System.out.println("asap message received (" + format + " | " + uri + "). size == " + messages.size());
            Iterator<byte[]> yourPDUIter = messages.getMessages();
            while (yourPDUIter.hasNext()) {
                ASAPMockUsage.deserializeExample(yourPDUIter.next());
            }
        }
    }
}
