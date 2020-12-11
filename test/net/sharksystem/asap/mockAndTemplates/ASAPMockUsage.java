package net.sharksystem.asap.mockAndTemplates;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.apps.ASAPSimplePeer;
import net.sharksystem.asap.apps.mock.ASAPPeerMock;
import org.junit.Test;

import java.io.*;

/**
 * How to mock ASAP communication
 */
public class ASAPMockUsage {

    /* I'm sorry. I changed mock implementation - it closer to the real implementation now, see mockUsageExample2
    @Test
    public void usageTest1() throws IOException, ASAPException, InterruptedException {

        // example - this should be produced by your application
        byte[] serializedData = ASAPMockUsage.serializeExample(42, "don't panic", true);

        // now: ASAP is used to deliver those data - we mock it
        ASAPPeerMock asapPeerMock = new ASAPPeerMock();

        ASAPMessageSender asapMessageSender = asapPeerMock;

        asapMessageSender.sendASAPMessage(YOUR_APP_NAME, YOUR_URI, serializedData);

        // we simulated a sender - now, we need to simulate recipient

        // this should be replaced with your code - you must implement a listener.
        ASAPMessageReceivedListenerExample asapMessageReceivedListenerExample =
                new ASAPMessageReceivedListenerExample();

        // register your listener (or that mock) with asap connection mock
        asapPeerMock.addASAPMessageReceivedListener(YOUR_APP_NAME, asapMessageReceivedListenerExample);

        // simulate ASAP encounter
        asapPeerMock.connect();

        // give your app a moment to process
        Thread.sleep(1000);

        // add another message while still connected
        asapMessageSender.sendASAPMessage(YOUR_APP_NAME, YOUR_URI,
                ASAPMockUsage.serializeExample(43, "second message", false));

        asapPeerMock.disconnect();
        System.out.println("send message without connection");
        asapMessageSender.sendASAPMessage(YOUR_APP_NAME, YOUR_URI,
                ASAPMockUsage.serializeExample(44, "third message", false));
        asapMessageSender.sendASAPMessage(YOUR_APP_NAME, YOUR_URI,
                ASAPMockUsage.serializeExample(45, "forth message", false));
        Thread.sleep(1000);

        System.out.println("re-connect");
        asapPeerMock.connect();
        Thread.sleep(1000);
    }
     */

    @Test
    public void mockUsageExample2() throws IOException, ASAPException, InterruptedException {
        ///////////////// ALICE //////////////////////////////////////////////////////////////
        // setup mocked peer
        ASAPPeerMock alicePeerMock = new ASAPPeerMock(TestUtils.ALICE);

        // use this interface - it's important - same interface can be used for real app in java and android
        ASAPSimplePeer alicePeer = alicePeerMock;

        // setup message received listener - this should be replaced with your code - you implement a listener.
        ASAPMessageReceivedListenerExample aliceMessageReceivedListenerExample =
                new ASAPMessageReceivedListenerExample();

        alicePeer.addASAPMessageReceivedListener(TestUtils.YOUR_APP_NAME, aliceMessageReceivedListenerExample);

        // example - this should be produced by your application
        byte[] serializedData = TestUtils.serializeExample(42, "from alice", true);

        alicePeer.sendASAPMessage(TestUtils.YOUR_APP_NAME, TestUtils.YOUR_URI, serializedData);

        ///////////////// BOB //////////////////////////////////////////////////////////////
        ASAPPeerMock bobPeerMock = new ASAPPeerMock(TestUtils.BOB);

        // use this interface - it's important - same interface can be used for real app in java and android
        ASAPSimplePeer bobPeer = bobPeerMock;

        // this should be replaced with your code - you must implement a listener.
        ASAPMessageReceivedListenerExample asapMessageReceivedListenerExample =
                new ASAPMessageReceivedListenerExample();

        // register your listener (or that mock) with asap connection mock
        bobPeer.addASAPMessageReceivedListener(TestUtils.YOUR_APP_NAME, asapMessageReceivedListenerExample);

        // bob writes something
        bobPeer.sendASAPMessage(TestUtils.YOUR_APP_NAME, TestUtils.YOUR_URI,
                TestUtils.serializeExample(43, "from bob", false));
        bobPeer.sendASAPMessage(TestUtils.YOUR_APP_NAME, TestUtils.YOUR_URI,
                TestUtils.serializeExample(44, "from bob again", false));

        // simulate ASAP first encounter
        System.out.println("+++++++++++++++++++ 1st encounter starts soon ++++++++++++++++++++");
        Thread.sleep(50);
        alicePeerMock.startEncounter(bobPeerMock);

        // give your app a moment to process
        Thread.sleep(1000);

        // stop encounter
        bobPeerMock.stopEncounter(alicePeerMock);

        // bob writes something
        bobPeerMock.sendASAPMessage(TestUtils.YOUR_APP_NAME, TestUtils.YOUR_URI,
                TestUtils.serializeExample(43, "third message from bob", false));

        // simulate second encounter
        System.out.println("+++++++++++++++++++ 2nd encounter starts soon ++++++++++++++++++++");
        Thread.sleep(50);
        alicePeerMock.startEncounter(bobPeerMock);

        // give your app a moment to process
        Thread.sleep(1000);
    }
}
