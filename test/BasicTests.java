import helper.TestReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import net.sharksystem.asp3.ASP3Engine;
import net.sharksystem.asp3.ASP3EngineFS;
import net.sharksystem.asp3.ASP3Exception;
import net.sharksystem.util.localloop.TCPChannel;
import org.junit.Test;
import net.sharksystem.asp3.ASP3ChunkStorage;
import org.junit.Assert;

/**
 * Here are some basic tests and usage examples.
 * @author thsc
 */
public class BasicTests {
    public static final String ALICE_BOB_CHAT_URL = "content://aliceAndBob.talk";
    public static final String ALICE_FOLDER = "alice";
    public static final String BOB_FOLDER = "bob";
    
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
    public void usage() throws IOException, ASP3Exception, InterruptedException {
        
        
       // create folder
       this.removeDirectory(ALICE_FOLDER); // clean previous version before
       File folder = new File(ALICE_FOLDER);
       folder.mkdir();
       
       this.removeDirectory(BOB_FOLDER);
       folder = new File(BOB_FOLDER);
       folder.mkdir();

       TestReader aliceReader = new TestReader("Alice");
       TestReader bobReader = new TestReader("Bob");

       ASP3ChunkStorage aliceWriter = ASP3EngineFS.getASP3Engine("Alice", ALICE_FOLDER, aliceReader);

       String messageAlice2Bob = "hallo Bob";
       aliceWriter.add(ALICE_BOB_CHAT_URL, messageAlice2Bob);


       ASP3ChunkStorage bobWriter = ASP3EngineFS.getASP3Engine("Bob", BOB_FOLDER, bobReader);

       String messageBob2Alice = "hi Alice";
       bobWriter.add(ALICE_BOB_CHAT_URL, messageBob2Alice);

       // sync
       ASP3Engine aliceEngine = ASP3EngineFS.getASP3Engine(ALICE_FOLDER, aliceReader);
       ASP3Engine bobEngine = ASP3EngineFS.getASP3Engine(BOB_FOLDER, bobReader);

       // create shared buffer
       /*
       BufferedStream alice2bob = new BufferedStream("a2b");
       BufferedStream bob2alice = new BufferedStream("b2a");
        */

       TCPChannel aliceChannel = new TCPChannel(7777, true, "a2b");
       TCPChannel bobChannel = new TCPChannel(7777, false, "b2a");
       
       aliceChannel.start();
       bobChannel.start();
       
       aliceChannel.waitForConnection();
       bobChannel.waitForConnection();
       
       // twist and run
       ASP3EngineThread aliceEngineThread = new ASP3EngineThread(aliceEngine, 
               aliceChannel.getInputStream(),
               aliceChannel.getOutputStream());

       aliceEngineThread.start();
       
       bobEngine.handleConnection(bobChannel.getInputStream(), bobChannel.getOutputStream());
       
       Thread.sleep(10000);
       
        // check received message
        List<String> messages = aliceReader.getMessages();
        Assert.assertEquals(1, messages.size());
        
        for(String message : messages) {
            // must be one 
            Assert.assertEquals(messageBob2Alice, message);
        }

        messages = bobReader.getMessages();
        Assert.assertEquals(1, messages.size());
        
        for(String message : bobReader.getMessages()) {
            // must be one 
            Assert.assertEquals(messageAlice2Bob, message);
        }
    }
}
