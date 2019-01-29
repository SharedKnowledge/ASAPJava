package net.sharksystem.aasp;

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
    public void chunksIter() throws IOException, AASPException {
        AASPEngine bobEngine = AASPEngineFS.getAASPEngine("bob");
        
        List<AASPChunk> chunks = bobEngine.getStorage().getChunks(0);
        Iterator<CharSequence> messages = chunks.iterator().next().getMessages();
        while(messages.hasNext()) {
            CharSequence message = messages.next();
            System.out.println(message);
        }
    }
}
