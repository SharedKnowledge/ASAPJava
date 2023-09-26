package howto;

import net.sharksystem.asap.*;
import net.sharksystem.asap.apps.TCPServerSocketAcceptor;
import net.sharksystem.utils.Log;
import net.sharksystem.utils.streams.StreamPair;
import net.sharksystem.utils.streams.StreamPairImpl;
import net.sharksystem.utils.tcp.SocketFactory;
import net.sharksystem.utils.tcp.StreamPairCreatedListener;
import net.sharksystem.utils.testsupport.TestConstants;
import net.sharksystem.utils.testsupport.TestHelper;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Problem: ASAPPeers can handle a connection. They are not meant to open a connection be their own. That's task of
 * another component. Our framework offers an ASAPTestPeerFS which allows setting up an encounter based on a
 * local loop (IP 127.0.0.1) TCP connection
 *
 * Real scenarios need to set up connections between two peers in different processes or most probably
 * different engines. You find examples here.
 *
 * See also https://github.com/SharedKnowledge/ASAPJava/wiki/EncounterManager
 *
 * @see net.sharksystem.asap.apps.testsupport.ASAPTestPeerFS
 */
public class ConnectPeers {

    private static final String TEST_FOLDER = "ConnectPeers";
    private CharSequence EXAMPLE_APP_FORMAT = "shark/x-connectPeersExample";

    /**
     * A single connection is set up and two peers run an ASAP encounter. It can work in test scenarios. It is not
     * a blueprint for a real application though - better use our encounter manager.
     * @throws IOException
     * @throws ASAPException
     * @throws InterruptedException
     */
    @Test
    public void connectAliceAndBob() throws IOException, ASAPException, InterruptedException {
        // supported formats
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(EXAMPLE_APP_FORMAT);

        // test folder for this test run
        String rootFolder = TestHelper.getFullTempFolderName(TEST_FOLDER, true);

        // set up alice
        String aliceFolder = rootFolder + "/" + TestConstants.ALICE_ID;
        ASAPPeerFS alicePeerFS = new ASAPPeerFS(TestConstants.ALICE_ID, aliceFolder, formats);
        // we only need connection handler capabilities in this scenario
        ASAPConnectionHandler alice = alicePeerFS;

        // set up bob
        String bobFolder = rootFolder + "/" + TestConstants.BOB_ID;
        ASAPConnectionHandler bob = new ASAPPeerFS(TestConstants.BOB_ID, bobFolder, formats);

        /* create a TCP connection. What we do here:
        We create a just single connection. We need a ServerSocket first and a Socket that connects to.

        A bit more complex but maybe more convenient solution can be found here:
        https://github.com/SharedKnowledge/ASAPJava/blob/master/src/main/java/net/sharksystem/asap/apps/testsupport/ASAPTestPeerFS.java
         */

        // get a port number for that test
        int portNumber = TestHelper.getPortNumber();

        ServerSocket serverSocket = new ServerSocket(portNumber);

        /*
            now. We need to accept incoming connection but have to create a socket at the same time...
            We need threads or a helper class.
         */
        SocketFactory socketFactory = new SocketFactory(serverSocket);
        Thread socketFactoryThread = new Thread(socketFactory);
        socketFactoryThread.start();

        // create a socket
        String addressRemotePeer = "localhost"; // change this in a real scenario
        Socket socket = new Socket(addressRemotePeer, portNumber);

        // give server side a moment to connect
        Thread.sleep(1);

        // it is an arbitrary choice - Alice takes socket side pairs
        alice.handleConnection(socket.getInputStream(), socket.getOutputStream());
        bob.handleConnection(socketFactory.getInputStream(), socketFactory.getOutputStream());

        // give it some time to run an encounter.
        Thread.sleep(5);
    }

    @Test
    public void connectAliceAndBob_2() throws IOException, ASAPException, InterruptedException {
        // supported formats
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(EXAMPLE_APP_FORMAT);

        // test folder for this test run
        String rootFolder = TestHelper.getFullTempFolderName(TEST_FOLDER, true);

        // set up alice
        String aliceFolder = rootFolder + "/" + TestConstants.ALICE_ID;
        ASAPPeerFS alicePeerFS = new ASAPPeerFS(TestConstants.ALICE_ID, aliceFolder, formats);
        // we only need connection handler capabilities in this scenario
        ASAPConnectionHandler alice = alicePeerFS;

        // set up bob
        String bobFolder = rootFolder + "/" + TestConstants.BOB_ID;
        ASAPConnectionHandler bob = new ASAPPeerFS(TestConstants.BOB_ID, bobFolder, formats);

        /* create a TCP connection. What we do here:
        We create a just single connection. We need a ServerSocket first and a Socket that connects to.

        A bit more complex but maybe more convenient solution can be found here:
        https://github.com/SharedKnowledge/ASAPJava/blob/master/src/main/java/net/sharksystem/asap/apps/testsupport/ASAPTestPeerFS.java
         */

        // get a port number for that test
        int portNumber = TestHelper.getPortNumber();

        /*
            now. We need to accept incoming connection but have to create a socket at the same time...
            We need threads or a helper class.
         */
        StreamPairCreatedListenerImpl aliceStreamPairCreatedListener = new StreamPairCreatedListenerImpl(alice);
        SocketFactory socketFactory = new SocketFactory(portNumber, aliceStreamPairCreatedListener);
        Thread socketFactoryThread = new Thread(socketFactory);
        socketFactoryThread.start();

        // create a socket
        String addressRemotePeer = "localhost"; // change this in a real scenario
        Socket socket = new Socket(addressRemotePeer, portNumber);

        // give server side a moment to connect
        Thread.sleep(1);

        // it is an arbitrary choice - Alice takes socket side pairs
        bob.handleConnection(socket.getInputStream(), socket.getOutputStream());

        // give it some time to run an encounter.
        Thread.sleep(5);
    }

    private class StreamPairCreatedListenerImpl implements StreamPairCreatedListener {
        private final ASAPConnectionHandler connectionHandler;

        StreamPairCreatedListenerImpl(ASAPConnectionHandler connectionHandler) {
            this.connectionHandler = connectionHandler;
        }

        @Override
        public void streamPairCreated(StreamPair streamPair) {
            try {
                this.connectionHandler.handleConnection(streamPair.getInputStream(), streamPair.getOutputStream());
            } catch (IOException | ASAPException e) {
                Log.writeLog(this, "problems handling connection: " + e.getLocalizedMessage());
            }
        }
    }

    @Test
    public void connectAliceAndBobWithEncounterManager_Preferred() throws IOException, ASAPException, InterruptedException {
        // supported formats
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(EXAMPLE_APP_FORMAT);

        // test folder for this test run
        String rootFolder = TestHelper.getFullTempFolderName(TEST_FOLDER, true);

        ////////////////////////// set up peers
        // set up alice
        String aliceFolder = rootFolder + "/" + TestConstants.ALICE_ID;
        ASAPConnectionHandler alice = new ASAPPeerFS(TestConstants.ALICE_ID, aliceFolder, formats);
        // set up bob
        String bobFolder = rootFolder + "/" + TestConstants.BOB_ID;
        ASAPConnectionHandler bob = new ASAPPeerFS(TestConstants.BOB_ID, bobFolder, formats);

        ////////////////////////// encounter manager
        ASAPEncounterManager aliceEncounterManager = new ASAPEncounterManagerImpl(alice);
        ASAPEncounterManager bobEncounterManager = new ASAPEncounterManagerImpl(bob);

        ////////////////////////// set up server socket and handle connection requests
        int portNumberAlice = TestHelper.getPortNumber();
        TCPServerSocketAcceptor aliceTcpServerSocketAcceptor =
                new TCPServerSocketAcceptor(portNumberAlice, aliceEncounterManager);

        int portNumberBob = TestHelper.getPortNumber();
        TCPServerSocketAcceptor bobTcpServerSocketAcceptor =
                new TCPServerSocketAcceptor(portNumberBob, bobEncounterManager);

        // give it a moment to settle
        Thread.sleep(5);

        // now, both side wit for connection establishment. Example

        // open connection to Bob
        Socket socket = new Socket("localhost", portNumberBob);

        // let Alice handle it
        aliceEncounterManager.handleEncounter(
                StreamPairImpl.getStreamPair(socket.getInputStream(), socket.getOutputStream()),
                ASAPEncounterConnectionType.INTERNET);

        // give it a moment to run ASAP session
        Thread.sleep(5);

        // There is just one peer in a real app.
    }
}
