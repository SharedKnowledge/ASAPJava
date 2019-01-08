package net.sharksystem.asp3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author thsc
 */
public class ASP3Engine implements ASP3ChunkStorage, ASP3ProtocolEngine {
    public static final String ANONYMOUS_OWNER = "anon";
    static String DEFAULT_OWNER = ANONYMOUS_OWNER;
    static int DEFAULT_INIT_ERA = 0;
    
    protected String owner = ANONYMOUS_OWNER;
    
    protected int era = 0;
    protected int oldestEra = 0;
    protected HashMap<String, Integer> lastSeen = new HashMap<>();
    protected ASP3Memento memento = null;
    
    private final ASP3Reader reader;
    /* private */ final ASP3Storage chunkStorage;

    ASP3Engine(ASP3Storage chunkStorage, ASP3Reader reader) 
            throws ASP3Exception, IOException {
        
        this.chunkStorage = chunkStorage;
        this.reader = reader;

/*        
        if(reader == null) {
            throw new ASP3Exception("reader must not be null");
        }
        */
    }

    ASP3Engine(ASP3Storage chunkStorage) throws ASP3Exception, IOException {
                this(chunkStorage, null);
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
        ASP3Chunk chunk = this.chunkStorage.getChunk(urlTarget, this.era);
        
        chunk.add(message);
    }
    
    @Override
    public ASP3Chunk getChunk(CharSequence urlTarget, int era) throws IOException {
        return this.chunkStorage.getChunk(urlTarget, era);
    }

    @Override
    public List<ASP3Chunk> getChunks(int era) throws IOException {
        return this.chunkStorage.getChunks(era);
    }

    @Override
    public void dropChunks(int era) throws IOException {
        this.chunkStorage.dropChunks(era);
    }

    //////////////////////////////////////////////////////////////////////
    //                       ProtocolEngine                             //
    //////////////////////////////////////////////////////////////////////
    
    List<String> activePeers = new ArrayList<>();
    
    private String getLogStart() {
        StringBuilder b = new StringBuilder();
        b.append("ASP3Engine (");
        b.append(this.owner);
        b.append(") ");

        return b.toString();
    } 
    
    @Override
    public void handleConnection(InputStream is, OutputStream os) {
        
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
                    new ASP3ChunkReader(this.reader, dis, this.owner, peer));
            
            readerThread.start();
            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("chunk reader started");
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

            int workingEra = this.getEraStartSync(peer);
            int currentEra = this.era;
            
            // we start a conversation - increase era
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

            do {

                List<ASP3Chunk> chunks = this.chunkStorage.getChunks(workingEra);
                //<<<<<<<<<<<<<<<<<<debug
                b = new StringBuilder();
                b.append(this.getLogStart());
                b.append("start iterating chunks with working Era: ");
                b.append(workingEra);
                System.out.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug

                for(ASP3Chunk chunk : chunks) {
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
                    if(!this.isPublic(chunk)) {
                        List<CharSequence> recipients = chunk.getRecipients();

                        if(!recipients.contains(peer)) {
                            continue;
                        }
                    }

                    //<<<<<<<<<<<<<<<<<<debug
                    b = new StringBuilder();
                    b.append(this.getLogStart());
                    b.append("send chunk");
                    //>>>>>>>>>>>>>>>>>>>debug
                    this.sendChunk(chunk, dos);

                    // remember sent
                    chunk.removeRecipient(peer);
                    //<<<<<<<<<<<<<<<<<<debug
                    b = new StringBuilder();
                    b.append(this.getLogStart());
                    b.append("removed recipient");
                    b.append(peer);
                    //>>>>>>>>>>>>>>>>>>>debug
                    // empty?
                    if(chunk.getRecipients().isEmpty()) {
                        chunk.drop();
                        //<<<<<<<<<<<<<<<<<<debug
                        b.append(this.getLogStart());
                        b.append("chunk dropped");
                    }
                }

                // make a breakpoint here
                if(this.memento != null) this.memento.save(this);

                // remember that we are in sync until that era
                this.setLastSeen(peer, workingEra);
                
                // next era which isn't necessarilly workingEra++ 
                workingEra = this.getNextEra(workingEra);

                // until served era we came in in the first place
            } while(workingEra != currentEra);
            
            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append(this.getLogStart());
            b.append("ended iterating chunks");
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug

            // TODO IMPORTANT!!!
            // sync incomming messagea as long as connected
        }
        catch(IOException ioe) {
            // TODO
            System.err.println("ioe: " + ioe.getLocalizedMessage());
        }
        finally {
            try {
                // remember that we drop conversation with that peer
                this.activePeers.remove(peer);
                
                dis.close();
                dos.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    @Override
    public int getNextEra(int workingEra) {
        if(workingEra == Integer.MAX_VALUE) {
            return 0;
        }
        
        return workingEra+1;
    }
    
    @Override
    public int getPreviousEra(int workingEra) {
        if(workingEra == 0) {
            return Integer.MAX_VALUE;
        }
        
        return workingEra-1;
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
    }
    
    private void sendChunk(ASP3Chunk chunk, DataOutputStream dos) 
            throws IOException {
        
        //<<<<<<<<<<<<<<<<<<debug
        StringBuilder b = new StringBuilder();
        b.append("ASP3Engine (");
        b.append(this.owner);
        b.append(")");
        b.append("send chunk url: ");
        b.append(chunk.getUri());
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>debug
        
        // send url
        dos.writeUTF(chunk.getUri());

        Iterator<CharSequence> messages = chunk.getMessages();
        //<<<<<<<<<<<<<<<<<<debug
        b = new StringBuilder();
        b.append("ASP3Engine (");
        b.append(this.owner);
        b.append(")");
        b.append("iterate messages ");
        System.out.println(b.toString());
        //>>>>>>>>>>>>>>>>>>>debug
        while(messages.hasNext()) {
            CharSequence message = messages.next();
            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append("ASP3Engine (");
            b.append(this.owner);
            b.append(")");
            b.append("read message: ");
            b.append(message);
            //>>>>>>>>>>>>>>>>>>>debug
            
            dos.writeUTF((String) message);

            //<<<<<<<<<<<<<<<<<<debug
            b = new StringBuilder();
            b.append("ASP3Engine (");
            b.append(this.owner);
            b.append(")");
            b.append("wrote message: ");
            b.append(message);
            //>>>>>>>>>>>>>>>>>>>debug

        }
        //<<<<<<<<<<<<<<<<<<debug
        b = new StringBuilder();
        b.append("ASP3Engine (");
        b.append(this.owner);
        b.append(")");
        b.append("stop iterating messages ");
        
    }

    /**
     * We interpret an existing chunk with *no* recipients as
     * public chunk
     * @param chunk
     * @return 
     */
    private boolean isPublic(ASP3Chunk chunk) {
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