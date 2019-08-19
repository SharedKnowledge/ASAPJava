package net.sharksystem.asap;

import net.sharksystem.asap.protocol.ASAP_1_0;
import net.sharksystem.asap.protocol.ASAP_AssimilationPDU_1_0;
import net.sharksystem.asap.protocol.ASAP_Modem_Impl;
import net.sharksystem.asap.protocol.ASAP_PDU_1_0;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Implements the chunk exchange step in the AAS protocol.
 * @author thsc42
 */

class ASAPChunkAssimilator implements Runnable {
    private final InputStream is;
    private final String peer;
    private final String owner;
    private final ASAPStorage storage;
    private final ASAPReceivedChunkListener listener;

    private final String logHead;

    ASAPChunkAssimilator(InputStream is, String owner,
                         String peer, ASAPStorage storage,
                         ASAPReceivedChunkListener listener) {
        this.is = is;
        this.peer = peer;
        this.owner = owner;
        this.storage = storage;
        this.listener = listener;

        this.logHead = this.getClass().getSimpleName();
    }

    private String getLogStart() {
        StringBuilder b = new StringBuilder();
        b.append(this.logHead);
        b.append(" (");
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
            // read asap assimiate messages
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append(" readChunks (sender: ");
            b.append(peer);
            b.append(") ");
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

            String logStart = b.toString();

            ASAP_1_0 protocol = new ASAP_Modem_Impl();

            while(true) { // until IOException informs end of communication
                // read assimilation message
                b = new StringBuilder();
                b.append(this.getLogStart());
                b.append(" readChunks (sender: ");
                b.append(peer);
                b.append(") going to read asap pdu");
                System.out.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug
                ASAP_PDU_1_0 asapPDU = protocol.readPDU(is);
                b = new StringBuilder();
                b.append(this.getLogStart());
                b.append(" readChunks (sender: ");
                b.append(peer);
                b.append(") successfully read pdu");
                System.out.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug

                if(asapPDU.getCommand() != ASAP_1_0.ASSIMILATE_CMD) {
                    //>>>>>>>>>>>>>>>>>>>debug
                    b = new StringBuilder();
                    b.append(this.getLogStart());
                    b.append(" readChunks (sender: ");
                    b.append(peer);
                    b.append(") no assimilation pdu received - fatal");
                    System.out.println(b.toString());
                    //>>>>>>>>>>>>>>>>>>>debug
                    throw new ASAPException("asap assimilate expected - fatal");
                }

                ASAP_AssimilationPDU_1_0 asapAssimilatePDU = (ASAP_AssimilationPDU_1_0) asapPDU;

                // read URI
                String uri = asapPDU.getChannel();

                ASAPChunkStorage incomingChunkStorage = storage.getIncomingChunkStorage(this.peer);
                ASAPChunk chunk = incomingChunkStorage.getChunk(uri, storage.getEra());

                if(chunk != null) {
                    //<<<<<<<<<<<<<<<<<<debug
                    b = new StringBuilder();
                    b.append(logStart);
                    b.append("got chunk: ");
                    b.append(uri);
                    System.out.println(b.toString());
                    //>>>>>>>>>>>>>>>>>>>debug
                } else {
                    //<<<<<<<<<<<<<<<<<<debug
                    b = new StringBuilder();
                    b.append(logStart);
                    b.append("ERROR: no chunk found for sender/uri: ");
                    b.append(" / ");
                    b.append(uri);
                    System.err.println(b.toString());
                    //>>>>>>>>>>>>>>>>>>>debug
                    throw new IOException("couldn't create local chunk storage - give up");
                }

                List<Integer> messageOffsets = asapAssimilatePDU.getMessageOffsets();

                // iterate messages and stream into chunk
                InputStream protocolInputStream = asapAssimilatePDU.getInputStream();
                long offset = 0;
                for(long nextOffset : messageOffsets) {
                    //<<<<<<<<<<<<<<<<<<debug
                    b = new StringBuilder();
                    b.append(logStart);
                    b.append("going to read message: [");
                    b.append(offset);
                    b.append(", ");
                    b.append(nextOffset);
                    b.append(")");
                    System.out.println(b.toString());
                    //>>>>>>>>>>>>>>>>>>>debug

                    chunk.addMessage(protocolInputStream, nextOffset - offset);

                    offset = nextOffset;
                }

                // last round
                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append(logStart);
                b.append("going to read last message: from offset ");
                b.append(offset);
                b.append(" to end of file - total length: ");
                b.append(asapAssimilatePDU.getLength());
                System.out.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug
                chunk.addMessage(protocolInputStream, asapAssimilatePDU.getLength() - offset);

                // read all messages
                listener.chunkReceived(peer, uri, storage.getEra());
            }
        }
        catch (IOException ex) {
            try {
                b = new StringBuilder();
                b.append(this.getLogStart());
                b.append("IOException (give up, close inputstream): ");
                b.append(ex.getLocalizedMessage());
                System.out.println(b.toString());

                is.close();
            }
            catch (IOException ex1) {   /* ignore */ }
        }
        catch (ASAPException e) {
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("ASAPException (give up, close inputstream): ");
            b.append(e.getLocalizedMessage());
            System.out.println(b.toString());
            try { is.close(); } catch (IOException ex1) {   /* ignore */ }
        }
    }
}
