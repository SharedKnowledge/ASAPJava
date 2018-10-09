/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sharksystem.asp3;

import java.io.IOException;
import java.util.Iterator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author thsc
 */
public class ChunkPrinter {
    
    @Test
    public void printChunk() throws IOException, ASP3Exception {
        String chunkTrunkName = "bob/0/content%3A%2F%2FaliceAndBob.talk";
        
        System.out.println("going to print content of chunk " + chunkTrunkName);
        
        try {
        ASP3ChunkFS chunk = new ASP3ChunkFS(null, chunkTrunkName);
        
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
