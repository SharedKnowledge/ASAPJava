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
    static void sendChunk(ASP3Chunk chunk, DataOutputStream dos)
            throws IOException {
        
        //<<<<<<<<<<<<<<<<<<debug
        StringBuilder b = new StringBuilder();
        b.append("send chunk url / #messages: ");
        b.append(chunk.getUri());
        b.append(" / ");
        b.append(chunk.getNumberMessage());
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>debug
        
        // send url
        dos.writeUTF(chunk.getUri());
        
        // send #messages
        dos.writeInt(chunk.getNumberMessage());

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
            
            // send message
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
    
    static void readChunk(ASP3ChunkStorage chunkStorage, ASP3Storage storage, 
            DataInputStream dis) throws IOException {
        
        try {
            // read URI
            String uri = dis.readUTF();
            
            // read number of messages
            int number = dis.readInt();
            
            //<<<<<<<<<<<<<<<<<<debug
            StringBuilder b = new StringBuilder();
            b.append("read chunkURI / #messages ");
            b.append(uri);
            b.append(" / ");
            b.append(number);
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug
            ASP3Chunk chunk = 
                    storage.getChunk(uri, chunkStorage.getEra());
            
            if(chunk != null) {
                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append("got chunk: ");
                b.append(uri);
                System.out.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug
            } else {
                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append("ERROR: no chunk found for sender/uri: ");
                b.append(" / ");
                b.append(uri);
                System.err.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug
                throw new IOException("couldn't create local chunk storage - give up");
            }

            for(;number > 0; number--) {
                // escapes with IOException
                String message = dis.readUTF();
                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append("chunk deserialisation read message: ");
                b.append(message);
                System.out.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug

                chunk.add(message);
            }
            
        } catch (IOException ex) {
            // done
        }
    }
}
