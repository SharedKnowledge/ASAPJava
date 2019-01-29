package net.sharksystem.aasp;

import net.sharksystem.aasp.AASPChunkFS;
import java.io.IOException;
import java.util.Iterator;
import net.sharksystem.aasp.AASPException;
import org.junit.Test;

/**
 *
 * @author thsc
 */
public class ChunkPrinter {
    
    @Test
    public void printChunk() throws IOException, AASPException {
        String chunkTrunkName = "bob/0/content%3A%2F%2FaliceAndBob.talk";
        
        System.out.println("going to print content of chunk " + chunkTrunkName);
        
        try {
        AASPChunkFS chunk = new AASPChunkFS(null, chunkTrunkName);
        
            System.out.println("url: " + chunk.getUri());
            
            Iterator<CharSequence> messages = chunk.getMessages();
            if(messages == null) {
                System.out.println("no message iterator");
                return;
            }
            
            int i = 0;
            while(messages.hasNext()) {
                CharSequence s = messages.next();
                StringBuilder b = new StringBuilder();
                b.append(i);
                b.append(": ");
                b.append(s);
                System.out.println(b.toString());
            }
        }
        catch(Throwable t) {
            System.out.println("failure");
            t.printStackTrace(System.out);
        }
    }
}
