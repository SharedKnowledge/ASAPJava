package net.sharksystem.asp3;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            ASP3ChunkSerialization.readChunk(this.storage, peerStorage, dis);
        } catch (IOException ex) {
            try {
                // give up
                dis.close();
            } catch (IOException ex1) {
                // ignore
            }
        }
    }
}
