package net.sharksystem.asap.sharknet;

import net.sharksystem.asap.ASAPEngineFS;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.util.ASAPPeerHandleConnectionThread;
import net.sharksystem.cmdline.TCPStream;
import net.sharksystem.crypto.BasicCryptoKeyStorage;
import net.sharksystem.crypto.BasicCryptoParameters;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.security.KeyPair;

public class SharkNetTests {
    public static final String WORKING_SUB_DIRECTORY = "sharkNetTests/";
    public static final String ALICE_ID = "Alice";
    public static final String BOB_ID = "Bob";
    public static final String ALICE_FOLDER = WORKING_SUB_DIRECTORY + "/" + ALICE_ID;
    public static final String BOB_FOLDER = WORKING_SUB_DIRECTORY + "/" + BOB_ID;
    public static final String MESSAGE = "Hi";
    public static final String URI = "sn2://all";
    public static final int EXAMPLE_PORT = 7070;

    @Test
    public void snTestEncryptedSignedOneMessage() throws ASAPException, IOException, InterruptedException {
        ASAPEngineFS.removeFolder(WORKING_SUB_DIRECTORY); // clean previous version before

        // Alice
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_ID);
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID); // Alice knows Bob

        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_ID, bobKeyPair);
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair()); // Bob knows Alice

        SharkNetPeer snPeerAlice = new SharkNetPeerFS(ALICE_ID, ALICE_FOLDER, keyStorageAlice);
        SharkNetMessageReceivedListener snListenerAlice = new SharkNetMessageReceivedListener();
        snPeerAlice.addSharkNetMessageListener(snListenerAlice);

        // one message signed and encrypted
        snPeerAlice.sendSharkNetMessage(MESSAGE.getBytes(), URI, BOB_ID, true, true);

        // GO AHEAD HERE
        SharkNetPeer snPeerBob = new SharkNetPeerFS(BOB_ID, BOB_FOLDER, keyStorageBob);
        SharkNetMessageReceivedListener snListenerBob = new SharkNetMessageReceivedListener();
        snPeerBob.addSharkNetMessageListener(snListenerBob);

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
        ASAPPeerHandleConnectionThread aliceThread = new ASAPPeerHandleConnectionThread(snPeerAlice,
                aliceStream.getInputStream(), aliceStream.getOutputStream());

        // alice is up and running in a thread
        aliceThread.start();

        // run bob in this test thread
        snPeerBob.handleConnection(bobStream.getInputStream(), bobStream.getOutputStream());
        Thread.sleep(1000);

        //Thread.sleep(Long.MAX_VALUE);

        Assert.assertEquals(1, snListenerAlice.receivedMessages.size());
        SharkNetMessage snMessage = snListenerBob.receivedMessages.get(0);
        Assert.assertTrue(snMessage.verified());
        Assert.assertTrue(snMessage.encrypted());
    }

    @Test
    public void snTestEncryptedSignedForVariants() throws ASAPException, IOException, InterruptedException {
        ASAPEngineFS.removeFolder(WORKING_SUB_DIRECTORY); // clean previous version before

        // Alice
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_ID);
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID); // Alice knows Bob

        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_ID, bobKeyPair);
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair()); // Bob knows Alice

        SharkNetPeer snPeerAlice = new SharkNetPeerFS(ALICE_ID, ALICE_FOLDER, keyStorageAlice);
        SharkNetMessageReceivedListener snListenerAlice = new SharkNetMessageReceivedListener();
        snPeerAlice.addSharkNetMessageListener(snListenerAlice);

        /*
         */
        snPeerAlice.sendSharkNetMessage(MESSAGE.getBytes(), URI, BOB_ID, false, false);
        snPeerAlice.sendSharkNetMessage(MESSAGE.getBytes(), URI, BOB_ID, true, false);
        snPeerAlice.sendSharkNetMessage(MESSAGE.getBytes(), URI, BOB_ID, false, true);
        snPeerAlice.sendSharkNetMessage(MESSAGE.getBytes(), URI, BOB_ID, true, true);

        // GO AHEAD HERE
        SharkNetPeer snPeerBob = new SharkNetPeerFS(BOB_ID, BOB_FOLDER, keyStorageBob);
        SharkNetMessageReceivedListener snListenerBob = new SharkNetMessageReceivedListener();
        snPeerBob.addSharkNetMessageListener(snListenerBob);

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
        ASAPPeerHandleConnectionThread aliceThread = new ASAPPeerHandleConnectionThread(snPeerAlice,
                aliceStream.getInputStream(), aliceStream.getOutputStream());

        // alice is up and running in a thread
        aliceThread.start();

        // run bob in this test thread
        snPeerBob.handleConnection(bobStream.getInputStream(), bobStream.getOutputStream());
        Thread.sleep(1000);

        //Thread.sleep(Long.MAX_VALUE);

        Assert.assertEquals(0, snListenerAlice.receivedMessages.size());
        Assert.assertEquals(4, snListenerBob.receivedMessages.size());
//        Assert.assertEquals(3, snListenerBob.receivedMessages.size());
        SharkNetMessage snMessage = snListenerBob.receivedMessages.get(0);
        Assert.assertEquals(ALICE_ID, snMessage.getSender());
        Assert.assertFalse(snMessage.verified());
        Assert.assertFalse(snMessage.encrypted());

        snMessage = snListenerBob.receivedMessages.get(1);
        Assert.assertTrue(snMessage.verified());
        Assert.assertFalse(snMessage.encrypted());

        snMessage = snListenerBob.receivedMessages.get(2);
        Assert.assertFalse(snMessage.verified());
        Assert.assertTrue(snMessage.encrypted());

        snMessage = snListenerBob.receivedMessages.get(3);
        Assert.assertTrue(snMessage.verified());
        Assert.assertTrue(snMessage.encrypted());
    }
}
