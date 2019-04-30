package net.sharksystem.asap;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Implements the chunk exchange step in the AAS protocol.
 * @author local
 */

class ASAPPChunkReader implements Runnable {
    private final DataInputStream dis;
    private final String peer;
    private final String owner;
    private final ASAPStorage storage;
    private final ASAPReceivedChunkListener listener;

    ASAPPChunkReader(DataInputStream dis, String owner,
                     String peer, ASAPStorage storage,
                     ASAPReceivedChunkListener listener) {
        this.dis = dis;
        this.peer = peer;
        this.owner = owner;
        this.storage = storage;
        this.listener = listener;
    }

    private String getLogStart() {
        StringBuilder b = new StringBuilder();
        b.append("ASAPPChunkReader (");
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
        
        ASAPChunkStorage peerStorage = null;
        
        // get received storage
        peerStorage = this.storage.getIncomingChunkStorage(peer);
        //<<<<<<<<<<<<<<<<<<debug
        b = new StringBuilder();
        b.append(this.getLogStart());
        b.append("got received chunk storage ");
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>debug
        
        try {
            ASAPChunkSerialization.readChunks(peer, this.storage,
                    peerStorage, dis, listener);
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
