package bugreports;

import net.sharksystem.CountsReceivedMessagesListener;
import net.sharksystem.SharkException;
import net.sharksystem.asap.*;
import net.sharksystem.asap.apps.TCPServerSocketAcceptor;
import net.sharksystem.fs.FSUtils;
import net.sharksystem.utils.streams.StreamPairImpl;
import net.sharksystem.utils.testsupport.TestConstants;
import net.sharksystem.utils.testsupport.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;


/**
 * This Test is a bug report for sending multi-hop messages.
 * There are three peers: Alice, Bob, and Clara. Alice and Clara are connected to Bob.
 *
 * Alice sends a message to Bob, and Bob forwards it to Clara.
 * The first test shows the bug.
 * If the message is sent after alice and clara have established a connection to bob and handled it, the message is
 * only received by Bob and is not forwarded to Clara.
 *
 * At the second test, clara handles the connection after the message from alice was received by bob.
 * The message is received by Bob and forwarded to Clara.
 */
public class ConnectPeersMultiHopTest {

    private static final String TEST_FOLDER = "ConnectPeers";
    private final CharSequence EXAMPLE_APP_FORMAT = "shark/x-connectPeersExample";

    private ASAPConnectionHandler alice;
    private ASAPConnectionHandler bob;
    private ASAPConnectionHandler clara;

    private ASAPEncounterManager aliceEncounterManager;
    private ASAPEncounterManager bobEncounterManager;
    private ASAPEncounterManager claraEncounterManager;

    @BeforeAll
    public static void removePreviousTestFolder() {
        FSUtils.removeFolder(TestConstants.ROOT_DIRECTORY + TEST_FOLDER);
    }

    @BeforeEach
    public void setUp() throws IOException, SharkException {
        // supported formats
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(EXAMPLE_APP_FORMAT);

        // test folder for this test run
        String rootFolder = TestHelper.getFullTempFolderName(TEST_FOLDER, true);

        ////////////////////////// set up peers
        // set up alice
        String aliceFolder = rootFolder + "/" + TestConstants.ALICE_ID;
        alice = new ASAPPeerFS(TestConstants.ALICE_ID, aliceFolder, formats);
        // set up bob
        String bobFolder = rootFolder + "/" + TestConstants.BOB_ID;
        bob = new ASAPPeerFS(TestConstants.BOB_ID, bobFolder, formats);
        // set up clara
        String claraFolder = rootFolder + "/" + TestConstants.CLARA_ID;
        clara = new ASAPPeerFS(TestConstants.CLARA_ID, claraFolder, formats);

        aliceEncounterManager = new ASAPEncounterManagerImpl(alice, TestConstants.ALICE_ID);
        bobEncounterManager = new ASAPEncounterManagerImpl(bob, TestConstants.BOB_ID);
        claraEncounterManager = new ASAPEncounterManagerImpl(clara, TestConstants.CLARA_ID);
    }

    @Test
    public void sendMessageMultiHopBug() throws IOException, SharkException, InterruptedException {

        ////////////////////////// set up server socket and handle connection requests
        int portNumberAlice = TestHelper.getPortNumber();
        TCPServerSocketAcceptor aliceTcpServerSocketAcceptor =
                new TCPServerSocketAcceptor(portNumberAlice, aliceEncounterManager);

        int portNumberBob = TestHelper.getPortNumber();
        TCPServerSocketAcceptor bobTcpServerSocketAcceptor =
                new TCPServerSocketAcceptor(portNumberBob, bobEncounterManager);

        // create second port for Bob, so that alice and clara can connect to Bob
        int portNumberBob2 = TestHelper.getPortNumber();
        TCPServerSocketAcceptor bobTcpServerSocketAcceptor2 =
                new TCPServerSocketAcceptor(portNumberBob2, bobEncounterManager);

        // setup message received listeners for bob and clara
        ASAPPeerFS bobPeerFS = (ASAPPeerFS) bob;
        CountsReceivedMessagesListener messageReceivedListenerBob = new CountsReceivedMessagesListener(TestConstants.BOB_ID);
        bobPeerFS.addASAPMessageReceivedListener(EXAMPLE_APP_FORMAT, messageReceivedListenerBob);

        ASAPPeerFS claraPeerFS = (ASAPPeerFS) clara;
        CountsReceivedMessagesListener messageReceivedListenerClara = new CountsReceivedMessagesListener(TestConstants.CLARA_ID);
        claraPeerFS.addASAPMessageReceivedListener(EXAMPLE_APP_FORMAT, messageReceivedListenerClara);

        // give it a moment to settle
        Thread.sleep(5);

        // now, bob opens two server sockets - one for alice and one for clara
        // open connection to Bob
        Socket socketAliceToBob = new Socket("localhost", portNumberBob);
        Socket socketClaraToBob = new Socket("localhost", portNumberBob2);


        // let Alice handle it
        aliceEncounterManager.handleEncounter(
                StreamPairImpl.getStreamPair(socketAliceToBob.getInputStream(), socketAliceToBob.getOutputStream()),
                ASAPEncounterConnectionType.INTERNET);
        // let clara handle it
        claraEncounterManager.handleEncounter(
                StreamPairImpl.getStreamPair(socketClaraToBob.getInputStream(), socketClaraToBob.getOutputStream()),
                ASAPEncounterConnectionType.INTERNET);
        // give it a moment to run ASAP session
        Thread.sleep(5000);

        // send message from Alice to Bob after peers handled the connection
        ASAPPeerFS alicePeerFS = (ASAPPeerFS) alice;
        alicePeerFS.sendASAPMessage(EXAMPLE_APP_FORMAT, "my-uri", "Hello Bob!".getBytes());

        // give it a moment for processing messages
        Thread.sleep(2000);

        Assertions.assertTrue(messageReceivedListenerBob.numberOfMessages > 0);
        Assertions.assertTrue(messageReceivedListenerClara.numberOfMessages > 0);
    }


    @Test
    public void sendMessageMultiHopGood() throws IOException, SharkException, InterruptedException {

        ////////////////////////// set up server socket and handle connection requests
        int portNumberAlice = TestHelper.getPortNumber();
        TCPServerSocketAcceptor aliceTcpServerSocketAcceptor =
                new TCPServerSocketAcceptor(portNumberAlice, aliceEncounterManager);

        int portNumberBob = TestHelper.getPortNumber();
        TCPServerSocketAcceptor bobTcpServerSocketAcceptor =
                new TCPServerSocketAcceptor(portNumberBob, bobEncounterManager);

        // create second port for Bob, so that alice and clara can connect to Bob
        int portNumberBob2 = TestHelper.getPortNumber();
        TCPServerSocketAcceptor bobTcpServerSocketAcceptor2 =
                new TCPServerSocketAcceptor(portNumberBob2, bobEncounterManager);

        // setup message received listeners for bob and clara
        ASAPPeerFS bobPeerFS = (ASAPPeerFS) bob;
        CountsReceivedMessagesListener messageReceivedListenerBob = new CountsReceivedMessagesListener(TestConstants.BOB_ID);
        bobPeerFS.addASAPMessageReceivedListener(EXAMPLE_APP_FORMAT, messageReceivedListenerBob);

        ASAPPeerFS claraPeerFS = (ASAPPeerFS) clara;
        CountsReceivedMessagesListener messageReceivedListenerClara = new CountsReceivedMessagesListener(TestConstants.CLARA_ID);
        claraPeerFS.addASAPMessageReceivedListener(EXAMPLE_APP_FORMAT, messageReceivedListenerClara);

        // give it a moment to settle
        Thread.sleep(5);

        // now, bob opens two server sockets - one for alice and one for clara
        // open connection to Bob
        Socket socketAliceToBob = new Socket("localhost", portNumberBob);
        Socket socketClaraToBob = new Socket("localhost", portNumberBob2);

        ASAPPeerFS alicePeerFS = (ASAPPeerFS) alice;
        alicePeerFS.sendASAPMessage(EXAMPLE_APP_FORMAT, "my-uri", "Hello Bob!".getBytes());
        // let Alice handle it
        aliceEncounterManager.handleEncounter(
                StreamPairImpl.getStreamPair(socketAliceToBob.getInputStream(), socketAliceToBob.getOutputStream()),
                ASAPEncounterConnectionType.INTERNET);

        Thread.sleep(2000);
        Assertions.assertTrue(messageReceivedListenerBob.numberOfMessages > 0);

        // handle connection after message from alice was received by bob
        claraEncounterManager.handleEncounter(
                StreamPairImpl.getStreamPair(socketClaraToBob.getInputStream(), socketClaraToBob.getOutputStream()),
                ASAPEncounterConnectionType.INTERNET);

        // give it a moment to run ASAP session
        Thread.sleep(2000);
        Assertions.assertTrue(messageReceivedListenerClara.numberOfMessages > 0);
    }

}