package net.sharksystem.asap.mock;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.apps.ASAPPeerServices;
import net.sharksystem.asap.apps.mock.ASAPPeerMock;
import org.junit.Test;

import java.io.*;

import static net.sharksystem.asap.mock.TestUtils.*;

public class ASAPSimplePeerUsage {
    @Test
    public void mockUsageExample2() throws IOException, ASAPException, InterruptedException {
        ///////////////// ALICE //////////////////////////////////////////////////////////////
        // setup mocked peer / asap application and activity in android
        ASAPPeerMock alicePeerMock = new ASAPPeerMock(ALICE);

        // use this interface - it's important - same interface can be used for real app in java and android
        ASAPPeerServices alicePeer = alicePeerMock;

        // setup message received listener - this should be replaced with your code - you implement a listener.
        ASAPMessageReceivedListenerExample aliceMessageReceivedListenerExample =
                new ASAPMessageReceivedListenerExample();

        alicePeer.addASAPMessageReceivedListener(YOUR_APP_NAME, aliceMessageReceivedListenerExample);

        // example - this should be produced by your application
        byte[] serializedData = TestUtils.serializeExample(42, "from alice", true);

        alicePeer.sendASAPMessage(YOUR_APP_NAME, YOUR_URI, serializedData);

        ///////////////// BOB //////////////////////////////////////////////////////////////
        // listens
        ASAPPeerMock bobPeerMock = new ASAPPeerMock(BOB);

        // use this interface - it's important - same interface can be used for real app in java and android
        ASAPPeerServices bobPeer = bobPeerMock;

        // this should be replaced with your code - you must implement a listener.
        ASAPMessageReceivedListenerExample asapMessageReceivedListenerExample =
                new ASAPMessageReceivedListenerExample();

        // register your listener (or that mock) with asap connection mock
        bobPeer.addASAPMessageReceivedListener(YOUR_APP_NAME, asapMessageReceivedListenerExample);

        // bob writes something
        bobPeer.sendASAPMessage(YOUR_APP_NAME, YOUR_URI,
                TestUtils.serializeExample(43, "from bob", false));
        bobPeer.sendASAPMessage(YOUR_APP_NAME, YOUR_URI,
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
        bobPeerMock.sendASAPMessage(YOUR_APP_NAME, YOUR_URI,
                TestUtils.serializeExample(43, "third message from bob", false));

        // simulate second encounter
        System.out.println("+++++++++++++++++++ 2nd encounter starts soon ++++++++++++++++++++");
        Thread.sleep(50);
        alicePeerMock.startEncounter(bobPeerMock);

        // give your app a moment to process
        Thread.sleep(1000);
    }
}
