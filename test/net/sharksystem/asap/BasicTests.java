package net.sharksystem.asap;

import java.io.IOException;
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
    public static final String ALICE_FOLDER = "alice";
    public static final String BOB_FOLDER = "bob";
    public static final String ALICE = "alice";
    public static final String BOB = "bob";
    public static final String ALICE2BOB_MESSAGE = "Hi Bob";
    public static final String BOB2ALICE_MESSAGE = "Hi Alice";
    
    @Test
    public void androidUsage() throws IOException, ASAPException, InterruptedException {
        ASAPEngineFS.removeFolder(ALICE_FOLDER); // clean previous version before
        ASAPEngineFS.removeFolder(BOB_FOLDER); // clean previous version before
       
        // alice writes a message into chunkStorage
        ASAPStorage aliceStorage =
                ASAPEngineFS.getASAPStorage(ALICE, ALICE_FOLDER);
        
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE);
        
        // bob does the same
        ASAPStorage bobStorage =
                ASAPEngineFS.getASAPStorage(BOB, BOB_FOLDER);
        
        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE);
        
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
        Thread.sleep(5000);
        
        // close connections: note ASAPEngine does NOT close any connection!!
        aliceChannel.close();
        bobChannel.close();
        Thread.sleep(1000);
        
        // check results
        
        // listener must have been informed about new messages
        Assert.assertTrue(aliceListener.chunkReceived());
        Assert.assertTrue(bobListener.chunkReceived());

        // get message alice received
        ASAPChunkStorage aliceSenderStored =
                aliceStorage.getIncomingChunkStorage(aliceListener.getSender());
        
        ASAPChunk aliceReceivedChunk =
                aliceSenderStored.getChunk(aliceListener.getUri(), 
                        aliceListener.getEra());
        
        CharSequence aliceReceivedMessage = 
                aliceReceivedChunk.getMessages().next();
        
        Assert.assertEquals(BOB2ALICE_MESSAGE, aliceReceivedMessage);
       
        // get message bob received
        ASAPChunkStorage bobSenderStored =
                bobStorage.getIncomingChunkStorage(bobListener.getSender());
        
        ASAPChunk bobReceivedChunk =
                bobSenderStored.getChunk(bobListener.getUri(), 
                        bobListener.getEra());
        
        CharSequence bobReceivedMessage = 
                bobReceivedChunk.getMessages().next();
        
        Assert.assertEquals(ALICE2BOB_MESSAGE, bobReceivedMessage);

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
