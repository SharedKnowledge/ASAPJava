package net.sharksystem.asap;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import net.sharksystem.asap.util.ASAPChunkReceiverTester;
import net.sharksystem.asap.util.ASAPEngineThread;
import net.sharksystem.util.localloop.TCPChannel;
import org.junit.Test;
import org.junit.Assert;

/**
 * Here are some basic tests and usage examples.
 * @author thsc
 */
public class BasicTests {
    public static final String ALICE_BOB_CHAT_URL = "content://aliceAndBob.talk";
    public static final String ALICE_FOLDER = "tests/alice";
    public static final String BOB_FOLDER = "tests/bob";
    public static final String ALICE = "alice";
    public static final String BOB = "bob";
    public static final String ALICE2BOB_MESSAGE = "Hi Bob";
    public static final String ALICE2BOB_MESSAGE2 = "Hi Bob again";
    public static final String BOB2ALICE_MESSAGE = "Hi Alice";
    public static final String BOB2ALICE_MESSAGE2 = "Hi Alice again";

    @Test
    public void androidUsage() throws IOException, ASAPException, InterruptedException {
        ASAPEngineFS.removeFolder(ALICE_FOLDER); // clean previous version before
        ASAPEngineFS.removeFolder(BOB_FOLDER); // clean previous version before
       
        // alice writes a message into chunkStorage
        ASAPStorage aliceStorage =
                ASAPEngineFS.getASAPStorage(ALICE, ALICE_FOLDER);

        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE);
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE2);

        // bob does the same
        ASAPStorage bobStorage =
                ASAPEngineFS.getASAPStorage(BOB, BOB_FOLDER);

        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE);
        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE2);

        // now set up both engines / use default reader
        ASAPEngine aliceEngine = ASAPEngineFS.getASAPEngine("Alice", ALICE_FOLDER);
        
        ASAPEngine bobEngine = ASAPEngineFS.getASAPEngine("Bob", BOB_FOLDER);
        
        ASAPChunkReceiverTester aliceListener = new ASAPChunkReceiverTester();
        ASAPChunkReceiverTester bobListener = new ASAPChunkReceiverTester();

        // create connections for both sides
        TCPChannel aliceChannel = new TCPChannel(7777, true, "a2b");
        TCPChannel bobChannel = new TCPChannel(7777, false, "b2a");

        aliceChannel.start();
        bobChannel.start();

        // wait to connect
        aliceChannel.waitForConnection();
        bobChannel.waitForConnection();
        
        // run engine as thread
        ASAPEngineThread aliceEngineThread = new ASAPEngineThread(aliceEngine,
                aliceChannel.getInputStream(),
                aliceChannel.getOutputStream(),
                aliceListener);

        aliceEngineThread.start();

        // and better debugging - no new thread
        bobEngine.handleConnection(bobChannel.getInputStream(), 
                bobChannel.getOutputStream(), bobListener);

        // wait until communication probably ends
        System.out.flush();
        System.err.flush();
        Thread.sleep(5000);
        System.out.flush();
        System.err.flush();

        // close connections: note ASAPEngine does NOT close any connection!!
        aliceChannel.close();
        bobChannel.close();
        System.out.flush();
        System.err.flush();
        Thread.sleep(1000);
        System.out.flush();
        System.err.flush();

        // check results
        
        // listener must have been informed about new messages
        Assert.assertTrue(aliceListener.chunkReceived());
        Assert.assertTrue(bobListener.chunkReceived());

        // get messages alice received
        ASAPChunkStorage aliceSenderStored =
                aliceStorage.getIncomingChunkStorage(aliceListener.getSender());
        
        ASAPChunk aliceReceivedChunk =
                aliceSenderStored.getChunk(aliceListener.getUri(), 
                        aliceListener.getEra());

        // #1
        Iterator<CharSequence> aliceReceivedMessages = aliceReceivedChunk.getMessages();
        CharSequence aliceReceivedMessage = aliceReceivedMessages.next();
        Assert.assertEquals(BOB2ALICE_MESSAGE, aliceReceivedMessage);
        // #2
        aliceReceivedMessage = aliceReceivedMessages.next();
        Assert.assertEquals(BOB2ALICE_MESSAGE2, aliceReceivedMessage);

        // get message bob received
        ASAPChunkStorage bobSenderStored =
                bobStorage.getIncomingChunkStorage(bobListener.getSender());
        
        ASAPChunk bobReceivedChunk =
                bobSenderStored.getChunk(bobListener.getUri(), 
                        bobListener.getEra());

        // #1
        Iterator<CharSequence> bobReceivedMessages = bobReceivedChunk.getMessages();
        CharSequence bobReceivedMessage = bobReceivedMessages.next();
        Assert.assertEquals(ALICE2BOB_MESSAGE, bobReceivedMessage);
        // #2
        bobReceivedMessage = bobReceivedMessages.next();
        Assert.assertEquals(ALICE2BOB_MESSAGE2, bobReceivedMessage);

        List<CharSequence> senderList = aliceStorage.getSender();
        // expect bob
        Assert.assertEquals(1, senderList.size());
        Assert.assertTrue(BOB.equalsIgnoreCase(senderList.get(0).toString()));

        // simulate a sync
        bobStorage = ASAPEngineFS.getASAPStorage(BOB, BOB_FOLDER);
        Assert.assertEquals(1, bobStorage.getEra());
        Assert.assertEquals(bobEngine.getEra(), bobStorage.getEra());
    }
}
