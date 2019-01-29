package net.sharksystem.aasp;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Implements the chunk exchange step in the AAS protocol.
 * @author local
 */

class AASPChunkReader implements Runnable {
    private final DataInputStream dis;
    private final String peer;
    private final String owner;
    private final AASPStorage storage;
    private final AASPReceivedChunkListener listener;

    AASPChunkReader(DataInputStream dis, String owner, 
            String peer, AASPStorage storage, 
            AASPReceivedChunkListener listener) {
        this.dis = dis;
        this.peer = peer;
        this.owner = owner;
        this.storage = storage;
        this.listener = listener;
    }

    private String getLogStart() {
        StringBuilder b = new StringBuilder();
        b.append("AASPChunkReader (");
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
        
        AASPChunkStorage peerStorage = null;
        
        // get received storage
        peerStorage = this.storage.getReceivedChunkStorage(peer);
        //<<<<<<<<<<<<<<<<<<debug
        b = new StringBuilder();
        b.append(this.getLogStart());
        b.append("got received chunk storage ");
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>debug
        
        try {
            AASPChunkSerialization.readChunks(peer, this.storage, 
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
