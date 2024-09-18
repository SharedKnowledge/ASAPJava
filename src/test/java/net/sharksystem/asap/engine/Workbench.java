package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPPeerHandleConnectionThread;
import net.sharksystem.asap.cmdline.ExampleASAPChunkReceivedListener;
import net.sharksystem.asap.cmdline.TCPStream;
import net.sharksystem.asap.crypto.InMemoASAPKeyStore;
import net.sharksystem.fs.FSUtils;
import org.junit.Test;

import java.io.*;
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
    public void scratch() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // sender. Ich sende long long, int long und teile das vorher mit
        long[] lValues = new long[3];
        int iValue = -1;
        lValues[0] = 1;
        lValues[1] = 2;
        lValues[2] = 3;

        int types = 0;

        // long comes first
        types += 0x01;
        types = types << 8;

        // another long
        types += 0x01;
        types = types << 8;

        // an int
        types += 0x00;
        types = types << 8;

        // another long
        types += 0x01;

        // sage was passiert
        dos.writeLong(types);

        dos.writeLong(lValues[0]);
        dos.writeLong(lValues[1]);
        dos.writeInt(iValue);
        dos.writeLong(lValues[2]);
        System.out.println("l1 == " + lValues[0] + " | l2 == " + lValues[1] + " | i1 == " + iValue + " | l3 == " + lValues[2]);

        // read
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);

        // init l index
        int lIndex = 0;

        // read Reihenfolge
        types = dis.readInt();
        int mask = 0xFF;
        mask = mask << 24;

        while (mask > 0) {
            if ((types & mask) != 0) {
                // long value
                lValues[lIndex++] = dis.readLong();
            } else {
                // int
                iValue = dis.readInt();
            }
            mask = mask >> 8;
        }
        System.out.println("l1 == " + lValues[0] + " | l2 == " + lValues[1] + " | i1 == " + iValue + " | l3 == " + lValues[2]);
    }

    @Test
    public void routeEncryptedMessage() throws IOException, ASAPException, InterruptedException {
        /*
        Alice produces an encrypted message with recipient Clara. It is sent to Bob. He cannot encrypt message,
        keeps and finally forwards it to Clara.
         */

        // Still something to do.

        // setup keystores
        InMemoASAPKeyStore keyStorageAlice = new InMemoASAPKeyStore(ALICE_PEER_NAME);

        // alice produces a key pair for alice. This would not work in real life
        KeyPair keyPairClara = keyStorageAlice.createTestPeer(CLARA_PEER_NAME);

        // there is a keystore but no key excepts Bobs' He cannot verify or encrypt anybody or anything
        InMemoASAPKeyStore keyStorageBob = new InMemoASAPKeyStore(BOB_PEER_NAME);

        InMemoASAPKeyStore keyStorageClara = new InMemoASAPKeyStore(CLARA_PEER_NAME, keyPairClara,
                System.currentTimeMillis());
        // clara knows Alice as well
        keyStorageClara.addKeyPair(ALICE_PEER_NAME, keyStorageAlice.getKeyPair());

        // clean up ASAP
        FSUtils.removeFolder(WORKING_SUB_DIRECTORY); // clean previous version before

        ///// Prepare Alice
        String aliceFolder = WORKING_SUB_DIRECTORY + ALICE_PEER_NAME;
        ExampleASAPChunkReceivedListener aliceChunkListener = new ExampleASAPChunkReceivedListener(aliceFolder);
        ASAPInternalPeer alicePeer = ASAPInternalPeerFS.createASAPPeer(ALICE_PEER_NAME, aliceFolder, aliceChunkListener);
        alicePeer.setASAPKeyStore(keyStorageAlice); // set keystore
        alicePeer.getASAPCommunicationControl().setSendEncryptedMessages(true); // send encrypted messages
        ASAPEngine aliceChatEngine = alicePeer.createEngineByFormat(APPNAME); // create engine

        // create a message
        String messageAlice = EXAMPLE_MESSAGE_STRING;
        byte[] messageBytes = messageAlice.getBytes();
        aliceChatEngine.add(CHAT_TOPIC, messageBytes); // and write to chat

        ///// Prepare Bob
        String bobFolder = WORKING_SUB_DIRECTORY + BOB_PEER_NAME;
        ExampleASAPChunkReceivedListener bobChunkListener = new ExampleASAPChunkReceivedListener(bobFolder);
        ASAPInternalPeer bobPeer = ASAPInternalPeerFS.createASAPPeer(BOB_PEER_NAME, bobFolder, bobChunkListener);
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
