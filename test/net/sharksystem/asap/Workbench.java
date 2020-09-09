package net.sharksystem.asap;

import net.sharksystem.asap.util.ASAPPeerHandleConnectionThread;
import net.sharksystem.cmdline.ExampleASAPChunkReceivedListener;
import net.sharksystem.cmdline.TCPStream;
import net.sharksystem.crypto.BasicCryptoKeyStorage;
import org.junit.Test;

import java.io.IOException;
import java.security.KeyPair;

public class Workbench {
    // copy back to UsageExamples
    public static final String WORKING_SUB_DIRECTORY = "cryptoTests/";
    public static final String ALICE_PEER_NAME = "Alice";
    public static final String BOB_PEER_NAME = "Bob";
    public static final String CLARA_PEER_NAME = "Clara";
    public static final String APPNAME = "encryptedChat";
    public static final String CHAT_TOPIC = "topicA";
    public static final int EXAMPLE_PORT = 7070;
    public static final String EXAMPLE_MESSAGE_STRING = "Hi";

    @Test
    public void routeEncryptedMessage() throws IOException, ASAPException, InterruptedException {
        /*
        Alice produces an encrypted message with recipient Clara. It is sent to Bob. He cannot encrypt message,
        keeps and finally forwards it to Clara.
         */

        // Still something to do.

        // setup keystores
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_PEER_NAME);

        // alice produces a key pair for alice. This would not work in real life
        KeyPair keyPairClara = keyStorageAlice.createTestPeer(CLARA_PEER_NAME);

        // there is a keystore but no key excepts Bobs' He cannot verify or encrypt anybody or anything
        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_PEER_NAME);

        BasicCryptoKeyStorage keyStorageClara = new BasicCryptoKeyStorage(CLARA_PEER_NAME, keyPairClara);
        // clara knows Alice as well
        keyStorageClara.addKeyPair(ALICE_PEER_NAME, keyStorageAlice.getKeyPair());

        // clean up ASAP
        ASAPEngineFS.removeFolder(WORKING_SUB_DIRECTORY); // clean previous version before

        ///// Prepare Alice
        String aliceFolder = WORKING_SUB_DIRECTORY + ALICE_PEER_NAME;
        ExampleASAPChunkReceivedListener aliceChunkListener = new ExampleASAPChunkReceivedListener(aliceFolder);
        ASAPPeer alicePeer = ASAPPeerFS.createASAPPeer(ALICE_PEER_NAME, aliceFolder, aliceChunkListener);
        alicePeer.setASAPBasicKeyStorage(keyStorageAlice); // set keystore
        alicePeer.getASAPCommunicationControl().setSendEncryptedMessages(true); // send encrypted messages
        ASAPEngine aliceChatEngine = alicePeer.createEngineByFormat(APPNAME); // create engine

        // create a message
        String messageAlice = EXAMPLE_MESSAGE_STRING;
        byte[] messageBytes = messageAlice.getBytes();
        aliceChatEngine.add(CHAT_TOPIC, messageBytes); // and write to chat

        ///// Prepare Bob
        String bobFolder = WORKING_SUB_DIRECTORY + BOB_PEER_NAME;
        ExampleASAPChunkReceivedListener bobChunkListener = new ExampleASAPChunkReceivedListener(bobFolder);
        ASAPPeer bobPeer = ASAPPeerFS.createASAPPeer(BOB_PEER_NAME, bobFolder, bobChunkListener);
        ASAPEngine bobChatEngine = bobPeer.createEngineByFormat(APPNAME);

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
        aliceThread.start();
        bobPeer.handleConnection(bobStream.getInputStream(), bobStream.getOutputStream());
        Thread.sleep(1000);

        // now Bob should have stored that encrypted message in a special store.

        int i = 42;

            /*
        // we assume the asap session was performed

        // bob chunk received listener must have received something
        List<ExampleASAPChunkReceivedListener.ASAPChunkReceivedParameters> receivedList =
                bobChunkListener.getReceivedList();
            Assert.assertTrue(receivedList.isEmpty());
         */
    }
}
