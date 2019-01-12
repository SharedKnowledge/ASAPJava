import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import net.sharksystem.asp3.ASP3Chunk2Send;
import net.sharksystem.asp3.ASP3Engine;
import net.sharksystem.asp3.ASP3EngineFS;
import net.sharksystem.asp3.ASP3Exception;
import net.sharksystem.util.localloop.TCPChannel;
import org.junit.Test;
import net.sharksystem.asp3.ASP3ChunkStorage;
import net.sharksystem.asp3.ASP3ReceivedChunkListener;
import net.sharksystem.asp3.ASP3Storage;
import org.junit.Assert;

/**
 * Here are some basic tests and usage examples.
 * @author thsc
 */
public class BasicTests {
    public static final String ALICE_BOB_CHAT_URL = "content://aliceAndBob.talk";
    public static final String ALICE_FOLDER = "alice";
    public static final String BOB_FOLDER = "bob";
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
            } else {
                file.delete();
            }
        }

        // finally remove directory itself
        dir.toFile().delete();
    }
    
    @Test
    public void androidUsage() throws IOException, ASP3Exception, InterruptedException {
       this.removeDirectory(ALICE_FOLDER); // clean previous version before
       this.removeDirectory(BOB_FOLDER); // clean previous version before
       
        // alice writes a message into chunkStorage
        ASP3ChunkStorage aliceStorage = 
                ASP3EngineFS.getASP3ChunkStorage(ALICE_FOLDER);
        
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE);
        
        // bob does the same
        ASP3ChunkStorage bobStorage = 
                ASP3EngineFS.getASP3ChunkStorage(BOB_FOLDER);
        
        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE);
        
        // now set up both engines / use default reader
        ASP3Engine aliceEngine = ASP3EngineFS.getASP3Engine("Alice", ALICE_FOLDER);
        
        ASP3Engine bobEngine = ASP3EngineFS.getASP3Engine("Bob", BOB_FOLDER);
        
        ASP3ChunkReceiverTester aliceListener = new ASP3ChunkReceiverTester();
        ASP3ChunkReceiverTester bobListener = new ASP3ChunkReceiverTester();

        // create connections for both sides
        TCPChannel aliceChannel = new TCPChannel(7777, true, "a2b");
        TCPChannel bobChannel = new TCPChannel(7777, false, "b2a");

        aliceChannel.start();
        bobChannel.start();

        // wait to connect
        aliceChannel.waitForConnection();
        bobChannel.waitForConnection();
        
        // run engine as thread
        ASP3EngineThread aliceEngineThread = new ASP3EngineThread(aliceEngine, 
                aliceChannel.getInputStream(),
                aliceChannel.getOutputStream(),
                aliceListener);

        aliceEngineThread.start();

        // and better debugging - no new thread
        bobEngine.handleConnection(bobChannel.getInputStream(), 
                bobChannel.getOutputStream(), bobListener);

        // wait until communication end
        Thread.sleep(10000);
        
        // listener must have been informed about new messages
        Assert.assertTrue(aliceListener.chunkReceived());
        Assert.assertTrue(bobListener.chunkReceived());

        // get message alice received
        ASP3Storage aliceSenderStored = 
                aliceStorage.getReceivedChunkStorage(aliceListener.getSender());
        
        ASP3Chunk2Send aliceReceivedChunk = 
                aliceSenderStored.getChunk(aliceListener.getUri(), 
                        aliceListener.getEra());
        
        CharSequence aliceReceivedMessage = 
                aliceReceivedChunk.getMessages().next();
        
        Assert.assertEquals(BOB2ALICE_MESSAGE, aliceReceivedMessage);
       
        // get message bob received
        ASP3Storage bobSenderStored = 
                bobStorage.getReceivedChunkStorage(bobListener.getSender());
        
        ASP3Chunk2Send bobReceivedChunk = 
                bobSenderStored.getChunk(bobListener.getUri(), 
                        bobListener.getEra());
        
        CharSequence bobReceivedMessage = 
                bobReceivedChunk.getMessages().next();
        
        Assert.assertEquals(ALICE2BOB_MESSAGE, bobReceivedMessage);
    }
}
