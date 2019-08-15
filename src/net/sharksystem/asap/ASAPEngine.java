package net.sharksystem.asap;

import net.sharksystem.asap.protocol.ASAP_1_0;
import net.sharksystem.asap.protocol.ASAP_Modem_Impl;
import net.sharksystem.asap.protocol.ASAP_PDU_1_0;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * That ASAPEngine manages exchange of stored messages with peers.
 * See ASPChunkStorage for details.
 * 
 * @see ASAPStorage
 * @author thsc
 */
public abstract class ASAPEngine implements ASAPStorage, ASAPProtocolEngine {
    public static final String ANONYMOUS_OWNER = "anon";
    static String DEFAULT_OWNER = ANONYMOUS_OWNER;
    static int DEFAULT_INIT_ERA = 0;
    
    /**
     * that engine transmitts serialized chunks to another peer.
     * After transmission that channel is closed. In immediate closure would
     * result in an IOException on the other side
     */
    private int sleepBeforeConnectionClosed = 5000; 
    
    protected String owner = ANONYMOUS_OWNER;
    
    protected int era = 0;
    protected int oldestEra = 0;
    protected HashMap<String, Integer> lastSeen = new HashMap<>();
    protected ASAPMemento memento = null;
    
    /* private */ final private ASAPChunkStorage chunkStorage;
    private boolean dropDeliveredChunks = false;

    protected ASAPEngine(ASAPChunkStorage chunkStorage)
            throws ASAPException, IOException {
        
        this.chunkStorage = chunkStorage;
    }
    
    @Override
    public ASAPChunkStorage getChunkStorage() {
        return this.chunkStorage;
    }
    
    ASAPChunkStorage getStorage() {
        return this.chunkStorage;
    }

    //////////////////////////////////////////////////////////////////////
    //                               Writer                             //
    //////////////////////////////////////////////////////////////////////
    
    @Override
    public void addRecipient(CharSequence urlTarget, CharSequence recipient) throws IOException {
        this.chunkStorage.getChunk(urlTarget, this.era).addRecipient(recipient);
    }

    @Override
    public void setRecipients(CharSequence urlTarget, List<CharSequence> recipients) throws IOException {
        this.chunkStorage.getChunk(urlTarget, this.era).setRecipients(recipients);
    }

    @Override
    public void removeRecipient(CharSequence urlTarget, CharSequence recipients) throws IOException {
        this.chunkStorage.getChunk(urlTarget, this.era).removeRecipient(recipients);
    }

    @Override
    public void add(CharSequence urlTarget, CharSequence message) throws IOException {
        ASAPChunk chunk = this.chunkStorage.getChunk(urlTarget, this.era);
        
        chunk.addMessage(message);
    }

    @Override
    public void add(CharSequence urlTarget, byte[] messageAsBytes) throws IOException {
        ASAPChunk chunk = this.chunkStorage.getChunk(urlTarget, this.era);

        chunk.addMessage(messageAsBytes);
    }

    //////////////////////////////////////////////////////////////////////
    //                       ProtocolEngine                             //
    //////////////////////////////////////////////////////////////////////
    
    List<String> activePeers = new ArrayList<>();
    
    private String getLogStart() {
        StringBuilder b = new StringBuilder();
        b.append("ASAPEngine (");
        b.append(this.owner);
        b.append(") ");

        return b.toString();
    }

    public void handleConnection_new(InputStream is, OutputStream os,
                                 ASAPReceivedChunkListener listener) {

        // TODO: HIER WEITERMACHEN!

        try {
            ASAP_1_0 protocol = new ASAP_Modem_Impl();

            // tell who I am - send a interest message
            protocol.interest(this.owner, null, null,
                    null, -1, -1, os, false);

            // read interest from the other side
            ASAP_PDU_1_0 asapPDU = protocol.readPDU(is);

            // must be an interest
            if(asapPDU.getCommand() != ASAP_1_0.INTEREST_CMD) {
                throw new ASAPException("protocol error: expected asap interest - got something else");
            }

            // get remote peer
            String peer = asapPDU.getPeer();

            //<<<<<<<<<<<<<<<<<<debug
            StringBuilder b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("read Peer: ");
            b.append(peer);
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

            // check conflict
            if(!this.permission2ProceedConversation(peer)) {
                throw new ASAPException("no permission to communicate with remote peer: " + peer);
            }

            // start reading from remote peer
            /*
            Thread readerThread = new Thread(
                    new ASAPPChunkReader(is, this.owner, peer, this, listener));

            readerThread.start();
*/
            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("chunk reader started");
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

            // era we are about to transmit
            int workingEra = this.getEraStartSync(peer);

            // newest era (which is not necessarily highest number!!)
            int currentEra = this.era;

            // we start a conversation - increment era for newly produced messages
            this.incrementEra();

            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("working era: ");
            b.append(workingEra);
            b.append(" / current era: ");
            b.append(currentEra);
            b.append(" / this.era: ");
            b.append(this.era);
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

            // make a breakpoint here
            if(this.memento != null) this.memento.save(this);
            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("memento saved");
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

            /*
            There is a little challenge: era uses a circle of numbers
            We cannot say: higher number, later era. That rule does *not*
            apply. We can calculate next era, though.

            That loop has to be re-entered as long as working era has not
            yet reached currentEra. In other words: lastRound is reached whenever
            workingEra == currentEra. Processing currentEra is the last round
            We at at least one round!
            */

            boolean lastRound = false; // assume more than one round
            do {
                lastRound = workingEra == currentEra;

                List<ASAPChunk> chunks = this.chunkStorage.getChunks(workingEra);
                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append(this.getLogStart());
                b.append("start iterating chunks with working Era: ");
                b.append(workingEra);
                System.out.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug

                for(ASAPChunk chunk : chunks) {
                    //<<<<<<<<<<<<<<<<<<debug
                    b = new StringBuilder();
                    b.append(this.getLogStart());
                    b.append("chunkUrl: ");
                    b.append(chunk.getUri());
                    b.append(" / isPublic: ");
                    b.append(this.isPublic(chunk));
                    System.out.println(b.toString());
                    //>>>>>>>>>>>>>>>>>>>debug

                    // is not a public chunk
                    if (!this.isPublic(chunk)) {
                        List<CharSequence> recipients = chunk.getRecipients();

                        if (!recipients.contains(peer)) {
                            continue;
                        }
                    }

                    //<<<<<<<<<<<<<<<<<<debug
                    b = new StringBuilder();
                    b.append(this.getLogStart());
                    b.append("send chunk");
                    System.out.println(b.toString());
                    //>>>>>>>>>>>>>>>>>>>debug

                    /*
    void assimilate(CharSequence peer, CharSequence recipientPeer, CharSequence format, CharSequence channel, int era,
                    int length, List<Integer> offsets, InputStream dataIS, OutputStream os, boolean signed)
            throws IOException, ASAPException;

                     */
                    // TODO: format
                    protocol.assimilate(this.owner, // peer
                            peer, // recipient
                            ASAP_1_0.ANY_FORMAT, // format: TODO - application like makan!!
                            chunk.getUri(), // channel ok
                            workingEra, // era ok
                            chunk.getLength(), // data length
                            chunk.getOffsetList(),
                            chunk.getMessageDataInputStream(),
                            os,
                            false);

                    // remember sent
                    chunk.removeRecipient(peer);
                    //<<<<<<<<<<<<<<<<<<debug
                    b = new StringBuilder();
                    b.append(this.getLogStart());
                    b.append("removed recipient ");
                    b.append(peer);
                    System.out.println(b.toString());
                    //>>>>>>>>>>>>>>>>>>>debug
                    // empty?
                    if (chunk.getRecipients().isEmpty()) {
                        if (this.isDropDeliveredChunks()) {
                            chunk.drop();
                            //<<<<<<<<<<<<<<<<<<debug
                            b = new StringBuilder();
                            b.append(this.getLogStart());
                            b.append("chunk dropped");
                            System.out.println(b.toString());
                        } else {
                            b = new StringBuilder();
                            b.append(this.getLogStart());
                            b.append("drop flag set false - engine does not remove delivered chunks");
                            System.out.println(b.toString());
                        }
                    }
                }

                // make a breakpoint here
                if(this.memento != null) this.memento.save(this);

                // remember that we are in sync until that era
                this.setLastSeen(peer, workingEra);

                // next era which isn't necessarilly workingEra++
                workingEra = this.getNextEra(workingEra);

                // as long as not already performed last round
            } while(!lastRound);

            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("ended iterating chunks");
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug
        }
        catch(Exception ioe) {
            //<<<<<<<<<<<<<<<<<<debug
            StringBuilder b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("IOEXception: ");
            b.append(ioe.getLocalizedMessage());
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug
        }
    }

    @Override
    public void handleConnection(InputStream is, OutputStream os,
            ASAPReceivedChunkListener listener) {
        // TODO: not a very bright idea to implement that code here
        // TODO: should use strategy pattern instead.
        
        DataInputStream dis = new DataInputStream(is);
        DataOutputStream dos = new DataOutputStream(os);
        
        String peer = null;
        
        try {
            // tell who I am
            dos.writeUTF(this.owner);
            // receive its designation
            peer = dis.readUTF();
            
            //<<<<<<<<<<<<<<<<<<debug
            StringBuilder b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("read Peer: ");
            b.append(peer);
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

            // check conflict
            if(!this.permission2ProceedConversation(peer)) {
                dis.close();
                dos.close();
                return;
            }
            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("permission ok");
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug
            
            // start reading from remote peer
            Thread readerThread = new Thread(
                    new ASAPPChunkReader(dis, this.owner, peer, this, listener));
            
            readerThread.start();
            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("chunk reader started");
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

            // era we are about to transmit
            int workingEra = this.getEraStartSync(peer);
            
            // newest era (which is not necessarily highest number!!)
            int currentEra = this.era;
            
            // we start a conversation - increment era for newly produced messages
            this.incrementEra();

            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("working era: ");
            b.append(workingEra);
            b.append(" / current era: ");
            b.append(currentEra);
            b.append(" / this.era: ");
            b.append(this.era);
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug
            
            // make a breakpoint here
            if(this.memento != null) this.memento.save(this);
            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("memento saved");
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug
            
            /*
            There is a little challenge: era uses a circle of numbers
            We cannot say: higher number, later era. That rule does *not*
            apply. We can calculate next era, though. 

            That loop has to be re-entered as long as working era has not 
            yet reached currentEra. In other words: lastRound is reached whenever
            workingEra == currentEra. Processing currentEra is the last round
            We at at least one round!
            */

            boolean lastRound = false; // assume more than one round
            do {
                lastRound = workingEra == currentEra;
                
                List<ASAPChunk> chunks = this.chunkStorage.getChunks(workingEra);
                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append(this.getLogStart());
                b.append("start iterating chunks with working Era: ");
                b.append(workingEra);
                System.out.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug

                for(ASAPChunk chunk : chunks) {
                    //<<<<<<<<<<<<<<<<<<debug
                    b = new StringBuilder();
                    b.append(this.getLogStart());
                    b.append("chunkUrl: ");
                    b.append(chunk.getUri());
                    b.append(" / isPublic: ");
                    b.append(this.isPublic(chunk));
                    System.out.println(b.toString());
                    //>>>>>>>>>>>>>>>>>>>debug

                    // is not a public chunk
                    if (!this.isPublic(chunk)) {
                        List<CharSequence> recipients = chunk.getRecipients();

                        if (!recipients.contains(peer)) {
                            continue;
                        }
                    }

                    //<<<<<<<<<<<<<<<<<<debug
                    b = new StringBuilder();
                    b.append(this.getLogStart());
                    b.append("send chunk");
                    System.out.println(b.toString());
                    //>>>>>>>>>>>>>>>>>>>debug
                    ASAPChunkSerialization.sendChunk(chunk, dos);

                    // remember sent
                    chunk.removeRecipient(peer);
                    //<<<<<<<<<<<<<<<<<<debug
                    b = new StringBuilder();
                    b.append(this.getLogStart());
                    b.append("removed recipient ");
                    b.append(peer);
                    System.out.println(b.toString());
                    //>>>>>>>>>>>>>>>>>>>debug
                    // empty?
                    if (chunk.getRecipients().isEmpty()) {
                        if (this.isDropDeliveredChunks()) {
                            chunk.drop();
                            //<<<<<<<<<<<<<<<<<<debug
                            b = new StringBuilder();
                            b.append(this.getLogStart());
                            b.append("chunk dropped");
                            System.out.println(b.toString());
                        } else {
                            b = new StringBuilder();
                            b.append(this.getLogStart());
                            b.append("drop flag set false - engine does not remove delivered chunks");
                            System.out.println(b.toString());
                        }
                    }
                }

                // make a breakpoint here
                if(this.memento != null) this.memento.save(this);

                // remember that we are in sync until that era
                this.setLastSeen(peer, workingEra);
                
                // next era which isn't necessarilly workingEra++ 
                workingEra = this.getNextEra(workingEra);

                // as long as not already performed last round
            } while(!lastRound);
            
            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("ended iterating chunks");
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug
        }
        catch(IOException ioe) {
            //<<<<<<<<<<<<<<<<<<debug
            StringBuilder b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("IOEXception: ");
            b.append(ioe.getLocalizedMessage());
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug
        }
        // connections are NOT close within that module but from caller!
//        finally {
//            try {
//                // remember that we drop conversation with that peer
//                this.activePeers.remove(peer);
//                
//                //<<<<<<<<<<<<<<<<<<debug
//                StringBuilder b = new StringBuilder();
//                b.append(this.getLogStart());
//                b.append("about closing output stream");
//                System.out.println(b.toString());
//                //>>>>>>>>>>>>>>>>>>>debug
//                try {
//                    Thread.sleep(sleepBeforeConnectionClosed);
//                } catch (InterruptedException ex) {
//                    // ignore
//                }
//                //dis.close();
//                dos.close();
//            } catch (IOException ex) {
//                // ignore
//            }
//        }
    }

    private boolean isDropDeliveredChunks() {
        return this.dropDeliveredChunks;
    }

    public void setDropDeliveredChunks(boolean drop) {
        this.dropDeliveredChunks = drop;
    }

    static int nextEra(int workingEra) {
        if(workingEra == Integer.MAX_VALUE) {
            return 0;
        }
        
        return workingEra+1;
    }
    
    @Override
    public int getNextEra(int workingEra) {
        return ASAPEngine.nextEra(workingEra);
    }

    static int previousEra(int workingEra) {
        if(workingEra == 0) {
            return Integer.MAX_VALUE;
        }
        
        return workingEra-1;
    }
    
    @Override
    public int getPreviousEra(int workingEra) {
        return ASAPEngine.previousEra(workingEra);
    }

    private int getEraStartSync(String peer) {
        Integer lastEra = this.lastSeen.get(peer);
        if(lastEra == null) {
            return this.getOldestEra();
        }
        
        return lastEra;
    }

    private void setLastSeen(String peer, int workingEra) {
        this.lastSeen.put(peer, era);
    }

    @Override
    public int getOldestEra() {
        return this.oldestEra;
    }
    
    @Override
    public int getEra() {
        return this.era;
    }
    
//    public int getNextEra(int era) {
//        return this.
//    }
    
    /**
     * Peers in ad-hoc networks have the tendency to establish two channels
     * simultaneously. It's due to the lack of a controlling server instance
     * and the - wanted - equality of each peers. We should suppress those
     * useless double conversations.
     * 
     * @param peer
     * @return 
     */
    private synchronized boolean permission2ProceedConversation(String peer) {
        // if that peer is not in the list - go ahead
        boolean goAhead = !this.activePeers.contains(peer);
        if(goAhead) {
            // add that peer - an other call will fail.
            this.activePeers.add(peer);
        }
        
        return goAhead;
    }

    private synchronized void incrementEra() throws IOException {
        // go to next era - even start with 0
        this.era = this.getNextEra(this.era);
        
        // drop very very old chunks - if available
        this.chunkStorage.dropChunks(this.era);

        // persistent values
        if(this.memento != null) this.memento.save(this);
    }
    
    /**
     * We interpret an existing chunk with *no* recipients as
     * public chunk
     * @param chunk
     * @return 
     */
    private boolean isPublic(ASAPChunk chunk) {
        return chunk.getRecipients().isEmpty();
    }

    @Override
    public void newEra() {
        try {
            this.incrementEra();
        } catch (IOException ex) {
            // TODO
        }
    }
}