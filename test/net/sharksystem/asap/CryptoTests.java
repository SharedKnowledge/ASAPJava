package net.sharksystem.asap;

import net.sharksystem.asap.util.ASAPPeerHandleConnectionThread;
import net.sharksystem.cmdline.ExampleASAPChunkReceivedListener;
import net.sharksystem.cmdline.TCPStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class CryptoTests {
    public static final String WORKING_SUB_DIRECTORY = "cryptoTests/";
    public static final String ALICE_PEER_NAME = "Alice";
    public static final String BOB_PEER_NAME = "Bob";
    public static final String CLARA_PEER_NAME = "Clara";
    public static final String APPNAME = "encryptedChat";
    public static final String CHAT_TOPIC = "topicA";
    public static final int EXAMPLE_PORT = 7070;
    public static final String EXAMPLE_MESSAGE_STRING = "Hi";

    @Test
    public void noExchangeNotSigned() throws IOException, ASAPException, InterruptedException {
        ASAPEngineFS.removeFolder(WORKING_SUB_DIRECTORY); // clean previous version before

        ///// Prepare Alice
        String aliceFolder = WORKING_SUB_DIRECTORY + ALICE_PEER_NAME;

        // ASAPChunkReceivedListener - an example
        ExampleASAPChunkReceivedListener aliceChunkListener = new ExampleASAPChunkReceivedListener(aliceFolder);

        // setup alice peer
        ASAPPeer alicePeer = ASAPPeerFS.createASAPPeer(ALICE_PEER_NAME, aliceFolder, aliceChunkListener);

        // setup chat on alice peer
        ASAPEngine aliceChatEngine = alicePeer.createEngineByFormat(APPNAME);
        // false is default but makes test more obvious
        aliceChatEngine.getASAPCommunicationControl().setSendEncryptedMessages(false);
        aliceChatEngine.getASAPCommunicationControl().setSendSignedMessages(false);

        // create a message
        String messageAlice = EXAMPLE_MESSAGE_STRING;

        // transform to bytes - there are more elaborate ways to produce a byte array of course
        byte[] messageBytes = messageAlice.getBytes();

        // write a message - we are still offline
        aliceChatEngine.add(CHAT_TOPIC, messageBytes);

        ///// Prepare Bob
        String bobFolder = WORKING_SUB_DIRECTORY + BOB_PEER_NAME;

        // ASAPChunkReceivedListener - an example
        ExampleASAPChunkReceivedListener bobChunkListener = new ExampleASAPChunkReceivedListener(bobFolder);

        // setup bob peer
        ASAPPeer bobPeer = ASAPPeerFS.createASAPPeer(BOB_PEER_NAME, bobFolder, bobChunkListener);

        // setup chat on alice peer
        ASAPEngine bobChatEngine = bobPeer.createEngineByFormat(APPNAME);
        // bob expects signed and encrypted what Alice not provides
        bobChatEngine.getASAPEnginePermissionSettings().setReceivedMessagesMustBeEncrypted(true);
        bobChatEngine.getASAPEnginePermissionSettings().setReceivedMessagesMustBeSigned(true);

        /////////////// create a connection - in real apps it is presumably a bluetooth wifi direct etc. connection
        // TCPStream is a helper class for connection establishment
        TCPStream aliceStream = new TCPStream(EXAMPLE_PORT, true, "alice2bob");
        TCPStream bobStream = new TCPStream(EXAMPLE_PORT, false, "b2a");

        // start tcp server or client and try to connect
        aliceStream.start();
        bobStream.start();

        // wait until connection is established
        aliceStream.waitForConnection();
        bobStream.waitForConnection();
        //////////////// end of connection establishment - a simulation in some way - but real enough. It is real tcp.

        // let both asap peers run an asap session
        ASAPPeerHandleConnectionThread aliceThread = new ASAPPeerHandleConnectionThread(alicePeer,
                aliceStream.getInputStream(), aliceStream.getOutputStream());

        // alice is up and running in a thread
        aliceThread.start();

        // run bob in this test thread
        bobPeer.handleConnection(bobStream.getInputStream(), bobStream.getOutputStream());

        // at this point give both asap engines some time to run their asap session - then we check what happened.
        Thread.sleep(1000);

        // we assume the asap session was performed

        // bob chunk received listener must have received something
        List<ExampleASAPChunkReceivedListener.ASAPChunkReceivedParameters> receivedList =
                bobChunkListener.getReceivedList();
        Assert.assertTrue(receivedList.isEmpty());

    }
}
