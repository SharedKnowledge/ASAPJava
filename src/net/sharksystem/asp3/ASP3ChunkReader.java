package net.sharksystem.asp3;

import java.io.DataInputStream;
import java.io.IOException;

class ASP3ChunkReader implements Runnable {
    private final DataInputStream dis;
    private final String peer;
    private final String owner;
    private final ASP3ChunkStorage storage;

    ASP3ChunkReader(DataInputStream dis, String owner, 
            String peer, ASP3ChunkStorage storage) {
        this.dis = dis;
        this.peer = peer;
        this.owner = owner;
        this.storage = storage;
    }

    private String getLogStart() {
        StringBuilder b = new StringBuilder();
        b.append("ASP3ChunkReader (");
        b.append(this.owner);
        b.append(") connected to (");
        b.append(this.peer);
        b.append(") ");

        return b.toString();
    }

    @Override
    public void run() {
        //<<<<<<<<<<<<<<<<<<debug
        StringBuilder b = new StringBuilder();
        b.append(this.getLogStart());
        b.append("start reading ");
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>debug
        
        ASP3Storage peerStorage = null;
        
        // get received storage
        peerStorage = this.storage.getReceivedChunkStorage(peer);
        //<<<<<<<<<<<<<<<<<<debug
        b = new StringBuilder();
        b.append(this.getLogStart());
        b.append("got received chunk storage ");
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>debug
       
        try {
            String uri = dis.readUTF();
            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("read chunkURI: ");
            b.append(uri);
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug
            ASP3Chunk2Send chunk = 
                    peerStorage.getChunk(uri, storage.getEra());
            
            if(chunk != null) {
                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append(this.getLogStart());
                b.append("got chunk: ");
                b.append(uri);
                System.out.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug
            } else {
                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append(this.getLogStart());
                b.append("ERROR: no chunk found for sender/uri: ");
                b.append(peer);
                b.append(" / ");
                b.append(uri);
                System.err.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug
            }

            ASP3ChunkSerialization.readChunk(chunk, dis);
            
            for(;;) {
                // escapes with IOException
                String message = dis.readUTF();
                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append(this.getLogStart());
                b.append("read message: ");
                b.append(message);
                System.out.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug

                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append(this.getLogStart());
                b.append("going to write to received chunk: ");
                b.append(uri);
                b.append(" / era: ");
                b.append(storage.getEra());
                System.out.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug

                chunk.add(message);
            }
        } catch (IOException ex) {
            // done
        }
    }
}
