package net.sharksystem.aasp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author thsc
 */
class AASPInMemoChunkCache implements AASPChunkCache {
    private final CharSequence uri;
    private final AASPChunkStorageFS chunkStorage;
    private final int fromEra;
    private final int toEra;

    private List<AASPChunk> chunkList;

    /** the internal message Cache */
    private List<CharSequence> messageCache;
    private int firstIndexMessageCache = -1;
    private int lastIndexMessageCache = -1;
    private int maxCacheLen = 1000;

    private int numberOfMessages = 0;

    public AASPInMemoChunkCache(AASPChunkStorageFS chunkStorage, 
            CharSequence uri, int fromEra, int toEra) {
        
        this.uri = uri;
        this.chunkStorage = chunkStorage;
        this.fromEra = fromEra;
        this.toEra = toEra;
    }
    
    private boolean initialized = false;
    
    private void initialize() throws IOException {
        if(!initialized) {
            this.syncChunkList();
            this.initialized = true;
        }
    }
    
    private void syncChunkList() throws IOException {
        // get all chunks in chronological order
        
        // current era in following loop
        int thisEra = this.fromEra;
        
        // do we need more than one loop?
        boolean anotherLoop = this.fromEra != this.toEra;
        
        // are we in the final loop?
        boolean finalLoop = !anotherLoop;
        
        // drop old vunk list - if any
        this.chunkList = new ArrayList<>();
        
        do {
            AASPChunk chunk = this.chunkStorage.getChunk(this.uri, thisEra);
            this.chunkList.add(chunk);
            this.numberOfMessages += chunk.getNumberMessage();
                
            if (anotherLoop) {
                if (finalLoop) {
                    anotherLoop = false;
                } else {
                    thisEra = AASPEngine.nextEra(thisEra);
                    finalLoop = thisEra == this.toEra;
                }
            }
        } while (anotherLoop);
        
    }
    
    @Override
    public int getNumberMessage() throws IOException {
        this.initialize();
        return this.numberOfMessages;
    }

    @Override
    public CharSequence getURI() {
        return this.uri;
    }

    @Override
    public Iterator<CharSequence> getMessages(boolean chronologically) throws IOException {
        this.initialize();

        List<CharSequence> dummyList = new ArrayList<>();
        
        dummyList.add("dummy entry 1");
        dummyList.add("dummy entry 2");
        
        return dummyList.iterator();
    }

    @Override
    public CharSequence getMessage(int position, boolean chronologically) 
            throws AASPException, IOException {

        this.initialize();

        if(position > this.numberOfMessages)
            throw new AASPException("Position exceeds number of message");

        if(!chronologically) {
            // invert position - first becomes last etc.
            position = this.numberOfMessages - 1 - position;
        }

        if(this.messageCache != null && position >= this.firstIndexMessageCache && position <= this.lastIndexMessageCache) {
            return this.messageCache.get(position - this.firstIndexMessageCache); // TODO calculation correct?
        }

        // not yet in cache - find chunk with required message
        int firstIndex = 0; // absolut index of first message in current chunk
        int lastIndex = 0; // absolut index of last message in current chunk

        boolean found = false;
        AASPChunk fittingChunk = null;
        int fittingChunkIndex = 0;

        for(AASPChunk chunk : this.chunkList) {
            lastIndex = firstIndex + chunk.getNumberMessage();

            if(position >= firstIndex && position <= lastIndex) {
                // we have got our chunk
                fittingChunk = chunk;
                break;
            }

            fittingChunkIndex++;
        }

        if(fittingChunk == null) {
            throw new AASPException("internal failure - wrong calculation in chunk cache");
        }

        // we can fill our cache right now

        // reset cache
        this.messageCache = new ArrayList<>();

        // simple approach in that first implementation ... we keep fitting chunk in memory
        Iterator<CharSequence> messages = fittingChunk.getMessages();
        this.firstIndexMessageCache = firstIndex;

        int counter = 0;
        while(messages.hasNext()) {
            this.messageCache.add(messages.next());
            counter++;
            if(counter > this.maxCacheLen) break;
        }

        this.lastIndexMessageCache = this.firstIndexMessageCache + counter - 1;

        // cache filled - call again
        return this.getMessage(position, chronologically);
    }

    @Override
    public void add(CharSequence message) throws IOException {
        // TODO
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

