package net.sharksystem.asap;

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
    public void chunksIter() throws IOException, ASAPException {
        ASAPEngine bobEngine = ASAPEngineFS.getASAPEngine("bob");
        
        List<ASAPChunk> chunks = bobEngine.getStorage().getChunks(0);
        Iterator<CharSequence> messages = chunks.iterator().next().getMessages();
        while(messages.hasNext()) {
            CharSequence message = messages.next();
            System.out.println(message);
        }
    }
}
