package net.sharksystem.asap.mock;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.apps.ASAPPeerServices;
import net.sharksystem.asap.apps.mock.ASAPPeerMock;
import net.sharksystem.asap.apps.mock.ASAPSimplePeer;
import org.junit.Test;

import java.io.*;

import static net.sharksystem.asap.mock.TestUtils.*;

/**
 * An ASAP app communicates by sending messages. First, you must ensure stability in your application.
 * Implement methods that serialized and deserialize messages. Implement a listener. Test your app
 * by testing scenarios. This class comprises a scenario in two steps.
 *
 * One tests uses an asap mock. It simulates a message exchange but does not use ASAP at all.
 *
 * The asapTestExamples is nearly identical with one important difference: The ASAP engines are used.
 *
 * Note: The test scenarios do not differ at all. Your application and test logic is written once and is tested
 * against a mock and later against ASAP. Same interfaces are also available in Android.  YOu can spent some
 * times by implementing test scenarios. Makes app coding on your target platform much faster.
 *
 * Test your app first with the mock and afterwards with the ASAP protocol stack. If anything runs smoothly -
 * you will have a stable Android or Java app in no time.
 */
public class UseThisAsTemplate4YourAppTests {
    private static final int PORT = 7777;

    private static int port = 0;
    static int getPortNumber() {
        if(UseThisAsTemplate4YourAppTests.port == 0) {
            UseThisAsTemplate4YourAppTests.port = PORT;
        } else {
            UseThisAsTemplate4YourAppTests.port++;
        }

        return UseThisAsTemplate4YourAppTests.port;
    }

    @Test
    public void mockTestExample() throws IOException, ASAPException, InterruptedException {
        ///////////////// ALICE //////////////////////////////////////////////////////////////
        // setup mocked peer / asap application and activity in android
        ASAPPeerMock aliceMockPeer = new ASAPPeerMock(ALICE);
        ASAPPeerMock bobMockPeer = new ASAPPeerMock(BOB);

        // 1st encounter
        this.scenarioPart1(aliceMockPeer, bobMockPeer);

        aliceMockPeer.startEncounter(bobMockPeer);
        // give your app a moment to process
        Thread.sleep(1000);
        // stop encounter
        bobMockPeer.stopEncounter(aliceMockPeer);
        // give your app a moment to process
        Thread.sleep(1000);

        // 2nd encounter
        this.scenarioPart2(aliceMockPeer, bobMockPeer);

        bobMockPeer.startEncounter(bobMockPeer);
    }

    @Test
    public void asapTestExample() throws IOException, ASAPException, InterruptedException {
        ///////////////// ALICE //////////////////////////////////////////////////////////////
        // setup mocked peer / asap application and activity in android
        ASAPSimplePeer aliceSimplePeer = new ASAPSimplePeer(ALICE);
        ASAPSimplePeer bobSimplePeer = new ASAPSimplePeer(BOB);

        // 1st encounter
        this.scenarioPart1(aliceSimplePeer, bobSimplePeer);

        aliceSimplePeer.startEncounter(getPortNumber(), bobSimplePeer);
        // give your app a moment to process
        Thread.sleep(1000);
        // stop encounter
        bobSimplePeer.stopEncounter(aliceSimplePeer);
        // give your app a moment to process
        Thread.sleep(1000);

        // 2nd encounter
        this.scenarioPart2(aliceSimplePeer, bobSimplePeer);

        aliceSimplePeer.startEncounter(getPortNumber(), bobSimplePeer);
    }

    public void scenarioPart1(ASAPPeerServices alicePeer, ASAPPeerServices bobPeer)
            throws IOException, ASAPException, InterruptedException {
        // simulate ASAP first encounter with full ASAP protocol stack and engines
        System.out.println("+++++++++++++++++++ 1st encounter starts soon ++++++++++++++++++++");
        Thread.sleep(50);

        // setup message received listener - this should be replaced with your code - you implement a listener.
        ASAPMessageReceivedListenerExample aliceMessageReceivedListenerExample =
                new ASAPMessageReceivedListenerExample();

        alicePeer.addASAPMessageReceivedListener(YOUR_APP_NAME, aliceMessageReceivedListenerExample);

        // example - this should be produced by your application
        byte[] serializedData = TestUtils.serializeExample(42, "from alice", true);

        alicePeer.sendASAPMessage(YOUR_APP_NAME, YOUR_URI, serializedData);

        ///////////////// BOB //////////////////////////////////////////////////////////////

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


        // give your app a moment to process
        Thread.sleep(500);
    }

    public void scenarioPart2(ASAPPeerServices alicePeer, ASAPPeerServices bobPeer)
            throws IOException, ASAPException, InterruptedException {

        // bob writes something
        bobPeer.sendASAPMessage(YOUR_APP_NAME, YOUR_URI,
                TestUtils.serializeExample(43, "third message from bob", false));

        // simulate second encounter
        System.out.println("+++++++++++++++++++ 2nd encounter starts soon ++++++++++++++++++++");
        Thread.sleep(50);
    }

    public void testScenarioResults() {
        // TODO
    }
}
