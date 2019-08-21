package net.sharksystem.asap;

import net.sharksystem.asap.protocol.*;

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

    protected String owner = ANONYMOUS_OWNER;
    protected String format = ASAP_1_0.ANY_FORMAT.toString();

    protected int era = 0;
    protected int oldestEra = 0;
    protected HashMap<String, Integer> lastSeen = new HashMap<>();
    protected ASAPMemento memento = null;
    
    /* private */ final private ASAPChunkStorage chunkStorage;
    private boolean dropDeliveredChunks = false;

    protected ASAPEngine(ASAPChunkStorage chunkStorage, CharSequence chunkContentFormat)
            throws ASAPException, IOException {
        
        this.chunkStorage = chunkStorage;
        if(chunkContentFormat != null) {
            this.format = chunkContentFormat.toString();
        } else {
            throw new ASAPException("format expected. like application/x-sn2-makan");
        }
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

    public void handleConnection(InputStream is, OutputStream os,
                                 ASAPReceivedChunkListener listener) {

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

            this.handleASAPInterest((ASAP_Interest_PDU_1_0) asapPDU, protocol, os);
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

    public void handleASAPOffer(ASAP_OfferPDU_1_0 asapOffer, ASAP_1_0 protocol, OutputStream os)
            throws ASAPException, IOException {
        //<<<<<<<<<<<<<<<<<<debug
        StringBuilder b = new StringBuilder();
        b.append(this.getLogStart());
        b.append("ASAP Offer is not handled in this implementation ");
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>debug
    }

    public void handleASAPAssimilate(ASAP_AssimilationPDU_1_0 asapAssimiliationPDU, ASAP_1_0 protocol,
                              InputStream is, OutputStream os, ASAPReceivedChunkListener listener)
            throws ASAPException, IOException {

        String sender = asapAssimiliationPDU.getPeer();
        //<<<<<<<<<<<<<<<<<<debug
        StringBuilder b = new StringBuilder();
        b.append(this.getLogStart());
        b.append("handle assimilate pdu received from ");
        b.append(sender);
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>debug

        // get received storage
        ASAPChunkStorage senderStorage = this.getIncomingChunkStorage(sender);
        //<<<<<<<<<<<<<<<<<<debug
        b = new StringBuilder();
        b.append(this.getLogStart());
        b.append("got received chunk storage for sender: ");
        b.append(sender);
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>debug

        try {
            // read URI
            String uri = asapAssimiliationPDU.getChannel();
            ASAPChunk chunk = senderStorage.getChunk(uri, this.getEra());

            if(chunk != null) {
                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append(this.getLogStart());
                b.append("got local chunk to store messages for uri: ");
                b.append(uri);
                System.out.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug
            } else {
                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append(this.getLogStart());
                b.append("ERROR: no chunk found for sender/uri: ");
                b.append(" / ");
                b.append(uri);
                System.err.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug
                throw new ASAPException("couldn't create local chunk storage - give up");
            }

            List<Integer> messageOffsets = asapAssimiliationPDU.getMessageOffsets();

            // iterate messages and stream into chunk
            InputStream protocolInputStream = asapAssimiliationPDU.getInputStream();
            long offset = 0;
            for(long nextOffset : messageOffsets) {
                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append(this.getLogStart());
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
            b.append(this.getLogStart());
            b.append("going to read last message: from offset ");
            b.append(offset);
            b.append(" to end of file - total length: ");
            b.append(asapAssimiliationPDU.getLength());
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

            chunk.addMessage(protocolInputStream, asapAssimiliationPDU.getLength() - offset);

            // read all messages
            listener.chunkReceived(sender, uri, this.getEra());
        }
        catch (IOException | ASAPException e) {
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("Exception (give up, keep streams untouched): ");
            b.append(e.getLocalizedMessage());
            System.out.println(b.toString());
            throw e;
        }
    }

    public void handleASAPInterest(ASAP_Interest_PDU_1_0 asapInterest, ASAP_1_0 protocol, OutputStream os)
            throws ASAPException, IOException {

        // get remote peer
        String peer = asapInterest.getPeer();

        //<<<<<<<<<<<<<<<<<<debug
        StringBuilder b = new StringBuilder();
        b.append(this.getLogStart());
        b.append("handle interest pdu received from ");
        b.append(peer);
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>debug

        // check conflict
        if(!this.permission2ProceedConversation(peer)) {
            throw new ASAPException("no permission to communicate with remote peer: " + peer);
        }

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

                protocol.assimilate(this.owner, // peer
                        peer, // recipient
                        this.format,
                        chunk.getUri(), // channel ok
                        workingEra, // era ok
                        chunk.getLength(), // data length
                        chunk.getOffsetList(),
                        chunk.getMessageInputStream(),
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

            // remember that we are in sync until that era
            this.setLastSeen(peer, workingEra);

            // make a breakpoint here
            if(this.memento != null) this.memento.save(this);

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