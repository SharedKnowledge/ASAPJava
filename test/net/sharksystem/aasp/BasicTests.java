package net.sharksystem.aasp;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    
    private void removeDirectory(String dirname) {
        Path dir = Paths.get(dirname);
        
        DirectoryStream<Path> entries = null;
        try {
            entries = Files.newDirectoryStream(dir);
        }
        catch(IOException ioe) {
            // directory does not exist - ok, nothing to drop
            return;
        }

        for (Path path : entries) {
            File file = path.toFile();
            if(file.isDirectory()) {
                this.removeDirectory(file.getAbsolutePath());
                boolean deleted = file.delete();
            } else {
                boolean deleted = file.delete();
            }
        }
        
        // finally remove directory itself
        File dirFile = new File(dirname);
        if(dirFile.exists()) {
            boolean deleted = dirFile.delete();
            int i = 42;
        }
    }
    
    @Test
    public void androidUsage() throws IOException, AASPException, InterruptedException {
       this.removeDirectory(ALICE_FOLDER); // clean previous version before
       this.removeDirectory(BOB_FOLDER); // clean previous version before
       
        // alice writes a message into chunkStorage
        AASPStorage aliceStorage = 
                AASPEngineFS.getAASPChunkStorage(ALICE_FOLDER);
        
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE);
        
        // bob does the same
        AASPStorage bobStorage = 
                AASPEngineFS.getAASPChunkStorage(BOB_FOLDER);
        
        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE);
        
        // now set up both engines / use default reader
        AASPEngine aliceEngine = AASPEngineFS.getAASPEngine("Alice", ALICE_FOLDER);
        
        AASPEngine bobEngine = AASPEngineFS.getAASPEngine("Bob", BOB_FOLDER);
        
        AASPChunkReceiverTester aliceListener = new AASPChunkReceiverTester();
        AASPChunkReceiverTester bobListener = new AASPChunkReceiverTester();

        // create connections for both sides
        TCPChannel aliceChannel = new TCPChannel(7777, true, "a2b");
        TCPChannel bobChannel = new TCPChannel(7777, false, "b2a");

        aliceChannel.start();
        bobChannel.start();

        // wait to connect
        aliceChannel.waitForConnection();
        bobChannel.waitForConnection();
        
        // run engine as thread
        AASPEngineThread aliceEngineThread = new AASPEngineThread(aliceEngine, 
                aliceChannel.getInputStream(),
                aliceChannel.getOutputStream(),
                aliceListener);

        aliceEngineThread.start();

        // and better debugging - no new thread
        bobEngine.handleConnection(bobChannel.getInputStream(), 
                bobChannel.getOutputStream(), bobListener);

        // wait until communication probably ends
        Thread.sleep(10000);
        
        // close connections: note AASPEngine does NOT close any connection!!
        aliceChannel.close();
        bobChannel.close();
        Thread.sleep(1000);
        
        // check results
        
        // listener must have been informed about new messages
        Assert.assertTrue(aliceListener.chunkReceived());
        Assert.assertTrue(bobListener.chunkReceived());

        // get message alice received
        AASPChunkStorage aliceSenderStored = 
                aliceStorage.getReceivedChunkStorage(aliceListener.getSender());
        
        AASPChunk aliceReceivedChunk = 
                aliceSenderStored.getChunk(aliceListener.getUri(), 
                        aliceListener.getEra());
        
        CharSequence aliceReceivedMessage = 
                aliceReceivedChunk.getMessages().next();
        
        Assert.assertEquals(BOB2ALICE_MESSAGE, aliceReceivedMessage);
       
        // get message bob received
        AASPChunkStorage bobSenderStored = 
                bobStorage.getReceivedChunkStorage(bobListener.getSender());
        
        AASPChunk bobReceivedChunk = 
                bobSenderStored.getChunk(bobListener.getUri(), 
                        bobListener.getEra());
        
        CharSequence bobReceivedMessage = 
                bobReceivedChunk.getMessages().next();
        
        Assert.assertEquals(ALICE2BOB_MESSAGE, bobReceivedMessage);
    }
}
