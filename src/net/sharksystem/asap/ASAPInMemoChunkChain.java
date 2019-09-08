package net.sharksystem.asap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 *
 * @author thsc
 */
class ASAPInMemoChunkChain implements ASAPChunkChain {
    public static final int DEFAULT_MAX_CACHE_SIZE = 1000;
    private final CharSequence uri;
    private final ASAPChunkStorageFS chunkStorage;
    private final int fromEra;
    private final int toEra;

    private List<ASAPChunk> chunkList;

    /** the internal message Cache */
    private List<CharSequence> messageCache;
    private int firstIndexMessageCache = -1;
    private int lastIndexMessageCache = -1;
    private int maxCacheLen;

    private int numberOfMessages = 0;

    public ASAPInMemoChunkChain(ASAPChunkStorageFS chunkStorage,
                                CharSequence uri, int fromEra, int toEra, int maxCacheLen) {

        this.uri = uri;
        this.chunkStorage = chunkStorage;
        this.fromEra = fromEra;
        this.toEra = toEra;
        this.maxCacheLen = maxCacheLen;
    }

    public ASAPInMemoChunkChain(ASAPChunkStorageFS chunkStorage,
                                CharSequence uri, int fromEra, int toEra) {

        this(chunkStorage, uri, fromEra, toEra, DEFAULT_MAX_CACHE_SIZE);
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
            // check if chunk exists - don't create on
            if(this.chunkStorage.existsChunk(this.uri, thisEra)) {
                // is there - get it
                ASAPChunk chunk = this.chunkStorage.getChunk(this.uri, thisEra);
                this.chunkList.add(chunk);
                this.numberOfMessages += chunk.getNumberMessage();
            }

            if (anotherLoop) {
                if (finalLoop) {
                    anotherLoop = false;
                } else {
                    thisEra = ASAPEngine.nextEra(thisEra);
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
    public Iterator<CharSequence> getMessages() throws IOException {
        this.initialize();

        return new ChunkListMessageIterator(this.chunkList);
    }

    @Override
    public CharSequence getMessage(int position, boolean chronologically) 
            throws ASAPException, IOException {

        this.initialize();

        if(position >= this.numberOfMessages)
            throw new ASAPException("Position exceeds number of message");

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
        ASAPChunk fittingChunk = null;
        int fittingChunkIndex = 0;

        for(ASAPChunk chunk : this.chunkList) {
            lastIndex = firstIndex + chunk.getNumberMessage() - 1;

            if(position >= firstIndex && position <= lastIndex) {
                // we have got our chunk
                fittingChunk = chunk;
                break;
            }

            firstIndex += chunk.getNumberMessage();
            fittingChunkIndex++;
        }

        if(fittingChunk == null) {
            throw new ASAPException("internal failure - wrong calculation in chunk cache");
        }

        // we can fill our cache right now

        // reset cache
        this.messageCache = new ArrayList<>();

        /////////////////////////////////////////////////////////////////
        // simple approach in that first implementation ... we keep fitting chunk in memory
        /////////////////////////////////////////////////////////////////

        Iterator<CharSequence> messages = fittingChunk.getMessages();

        // chunk bigger than max cache size?
        int chunkSize = fittingChunk.getNumberMessage();
        if(chunkSize > this.maxCacheLen) {
            // calculate how many messages to skip before caching

            /*
            situation:
            chunk head |................position...........................| tail
            cache head |................| tail

            solution: put position in middle of the cache
            chunk |................position...........................|
            planned cache |........position........|
            skipLen...|
             */

            int skipLen = position - (this.maxCacheLen / 2);

            // first index in cache will be this one:
            firstIndex += skipLen;

            // skip
            for(;skipLen > 0; skipLen--) {
                this.messageCache.add(messages.next());
            }
        }

        this.firstIndexMessageCache = firstIndex;

        int counter = 0;
        while(messages.hasNext()) {
            this.messageCache.add(messages.next());
            counter++;
            if(counter > this.maxCacheLen) break;
        }

        this.lastIndexMessageCache = this.firstIndexMessageCache + counter - 1;

        // cache filled - call again
        /* not: it is always chronologically true!!
        a) we already came in with true -> it remains true
        b) we came with false -> we have already recalculated that position, we would
        move it around again with that call - keep position unchanged: true!
         */

        return this.getMessage(position, true);
    }

    public void sync() throws IOException {
        this.initialized = false;
        this.firstIndexMessageCache = -1;
        this.lastIndexMessageCache = -1;
        this.numberOfMessages = 0;
        this.messageCache = null;
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //                          helper: message iterator implementation                     //
    //////////////////////////////////////////////////////////////////////////////////////////

    private class ChunkListMessageIterator implements Iterator<CharSequence> {

        private final List<ASAPChunk> chunkList;
        private ASAPChunk currentChunk;
        private int nextIndex;
        private Iterator<CharSequence> currentIterator;
        private CharSequence messageAhead;

        public ChunkListMessageIterator(List<ASAPChunk> chunkList) throws IOException {
            this.chunkList = chunkList;
            this.currentChunk = null;
            this.nextIndex = 0;
            this.messageAhead = null; // mark as empty
            this.readAhead(); // init cache
        }

        /**
         * read next message in field messageAhead.
         */
        private void readAhead() {
            if (this.currentIterator != null) {
                if (this.currentIterator.hasNext()) {
                    // 'normal' case: we read next message
                    this.messageAhead = this.currentIterator.next();
                    return; // done
                }
            }
            // no more messages in that iterator / chunk
            if (this.chunkList == null || nextIndex >= this.chunkList.size() ) {
                return; // there is no list at all or we are already through with it
            }

            // open next chunk / iterator
            this.currentChunk = this.chunkList.get(this.nextIndex++);
            try {
                this.currentIterator = this.currentChunk.getMessages();
                this.readAhead(); // next try
            } catch (IOException e) {
                // cannot recover from that problem
                return;
            }
        }

        @Override
        public boolean hasNext() {
            return this.messageAhead != null;
        }

        @Override
        public CharSequence next() {
            if(this.messageAhead == null) {
                throw new NoSuchElementException("list empty or already reached end");
            }

            // remove that single message cache
            CharSequence retMessage = this.messageAhead;
            this.messageAhead = null;

            // read ahead - if possible
            this.readAhead();

            return retMessage;
        }
    }

    public int size() throws IOException {
        this.initialize();
        return this.numberOfMessages;
    }
}
