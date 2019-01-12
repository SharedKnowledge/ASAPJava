package net.sharksystem.asp3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 *
 * @author thsc
 */
abstract class ASP3ChunkSerialization {
    static void sendChunk(ASP3Chunk2Send chunk, DataOutputStream dos)
            throws IOException {
        
        //<<<<<<<<<<<<<<<<<<debug
        StringBuilder b = new StringBuilder();
        b.append("send chunk url: ");
        b.append(chunk.getUri());
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>debug
        
        // send url
        dos.writeUTF(chunk.getUri());

        Iterator<CharSequence> messages = chunk.getMessages();
        //<<<<<<<<<<<<<<<<<<debug
        b = new StringBuilder();
        b.append("iterate messages ");
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>debug
        while(messages.hasNext()) {
            CharSequence message = messages.next();
            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append("read message from chunk: ");
            b.append(message);
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug
            
            dos.writeUTF((String) message);

            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append("wrote message to stream: ");
            b.append(message);
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

        }
        //<<<<<<<<<<<<<<<<<<debug
        b = new StringBuilder();
        b.append("stop iterating messages ");
        System.out.println(b.toString());
    }
    
    static void readChunk(ASP3ChunkReceived chunk, DataInputStream dis)
            throws IOException {
        
        for(;;) {
            // escapes with IOException
            String message = dis.readUTF();
            //<<<<<<<<<<<<<<<<<<debug
            StringBuilder b = new StringBuilder();
            b.append("chunk deserialisation read message: ");
            b.append(message);
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

            chunk.addReceivedMessage(message);
        }
    }
}
