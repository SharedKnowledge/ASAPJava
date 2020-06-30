package net.sharksystem.asap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

/**
 *
 * @author thsc
 */
public class ChunkPrinter {
    
    @Test
    public void printChunk() throws IOException, ASAPException {
        Map<String, String> m = new HashMap<>();
        m.put(null, "A");

        m.remove(null);

        String chunkTrunkName = "bob/0/content%3A%2F%2FaliceAndBob.talk";
        
        System.out.println("going to print content of chunk " + chunkTrunkName);
        
        try {
        ASAPChunkFS chunk = new ASAPChunkFS(null, chunkTrunkName);
        
            System.out.println("url: " + chunk.getUri());
            
            Iterator<CharSequence> messages = chunk.getMessagesAsCharSequence();
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
