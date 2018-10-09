package net.sharksystem.asp3;

import helper.TestReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author thsc
 */
public class WhiteBoxTests {
    
    public WhiteBoxTests() {
    }
    
    @Test
    public void chunksIter() throws IOException, ASP3Exception {
        TestReader bobReader = new TestReader("Bob");
        ASP3Engine bobEngine = ASP3EngineFS.getASP3Engine("bob", bobReader);
        
        List<ASP3Chunk> chunks = bobEngine.chunkStorage.getChunks(0);
        Iterator<CharSequence> messages = chunks.iterator().next().getMessages();
        while(messages.hasNext()) {
            CharSequence message = messages.next();
            System.out.println(message);
        }
    }
}
