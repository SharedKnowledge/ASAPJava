package howto;

import net.sharksystem.asap.ASAPConnectionHandler;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.ASAPPeerFS;
import net.sharksystem.utils.streams.StreamPair;
import net.sharksystem.utils.streams.StreamPairImpl;
import net.sharksystem.utils.tcp.SocketFactory;
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
    private int PORT_NUMBER = 7777;

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

        ServerSocket serverSocket = new ServerSocket(PORT_NUMBER);

        /*
            now. We need to accept incoming connection but have to create a socket at the same time...
            We need threads or a helper class.
         */
        SocketFactory socketFactory = new SocketFactory(serverSocket);
        Thread socketFactoryThread = new Thread(socketFactory);
        socketFactoryThread.start();

        // create a socket
        String addressRemotePeer = "localhost"; // change this in a real scenario
        Socket socket = new Socket(addressRemotePeer, PORT_NUMBER);

        // give server side a moment to connect
        Thread.sleep(1);

        // it is an arbitrary choice - Alice takes socket side pairs
        alice.handleConnection(socket.getInputStream(), socket.getOutputStream());
        bob.handleConnection(socketFactory.getInputStream(), socketFactory.getOutputStream());

        // give it some time to run an encounter.
        Thread.sleep(5);

    }
}
