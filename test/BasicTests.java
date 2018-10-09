import helper.TestReader;
import java.io.File;
import java.io.IOException;
import net.sharksystem.asp3.ASP3Engine;
import net.sharksystem.asp3.ASP3Writer;
import net.sharksystem.asp3.ASP3EngineFS;
import net.sharksystem.asp3.ASP3Exception;
import net.sharksystem.util.localloop.TCPChannel;
import org.junit.Test;

/**
 *
 * @author thsc
 */
public class BasicTests {
    public static final String ALICE_BOB_CHAT_URL = "content://aliceAndBob.talk";
    public static final String ALICE_FOLDER = "alice";
    public static final String BOB_FOLDER = "bob";
    
    @Test
    public void usage() throws IOException, ASP3Exception, InterruptedException {

       // create folder
       File folder = new File(ALICE_FOLDER);
       folder.mkdir();
       folder = new File(BOB_FOLDER);
       folder.mkdir();

       TestReader aliceReader = new TestReader("Alice");
       TestReader bobReader = new TestReader("Bob");

       ASP3Writer aliceWriter = ASP3EngineFS.getASP3Engine("Alice", ALICE_FOLDER, aliceReader);

       aliceWriter.add(ALICE_BOB_CHAT_URL, "hallo Bob");


       ASP3Writer bobWriter = ASP3EngineFS.getASP3Engine("Bob", BOB_FOLDER, bobReader);

       bobWriter.add(ALICE_BOB_CHAT_URL, "hi Alice");

       // sync
       ASP3Engine aliceEngine = ASP3EngineFS.getASP3Engine(ALICE_FOLDER, aliceReader);
       ASP3Engine bobEngine = ASP3EngineFS.getASP3Engine(BOB_FOLDER, bobReader);

       // create shared buffer
       /*
       BufferedStream alice2bob = new BufferedStream("a2b");
       BufferedStream bob2alice = new BufferedStream("b2a");
        */

       TCPChannel alice2bob = new TCPChannel(7777, true, "a2b");
       TCPChannel bob2alice = new TCPChannel(7777, false, "b2a");
       
       alice2bob.start();
       bob2alice.start();
       
       alice2bob.waitForConnection();
       bob2alice.waitForConnection();
       
       // twist and run
       ASP3EngineThread aliceEngineThread = new ASP3EngineThread(aliceEngine, 
               bob2alice.getInputStream(),
               alice2bob.getOutputStream());

       aliceEngineThread.start();
       
       bobEngine.handleConnection(alice2bob.getInputStream(), bob2alice.getOutputStream());
       
       Thread.sleep(2000);
       
    }
}
