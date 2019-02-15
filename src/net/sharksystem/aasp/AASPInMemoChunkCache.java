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
    private int size = 0;

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
        }
        
        this.initialized = true;
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
            this.size += chunk.getNumberMessage();
                
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
        return this.size;
    }

    @Override
    public CharSequence getURI() {
        return this.uri;
    }

    @Override
    public Iterator<CharSequence> getMessages(boolean chronologically) throws IOException {
        List<CharSequence> dummyList = new ArrayList<>();
        
        dummyList.add("dummy entry 1");
        dummyList.add("dummy entry 2");
        
        return dummyList.iterator();
    }

    @Override
    public CharSequence getMessage(int position, boolean chronologically) 
            throws AASPException, IOException {
        
        if(position > this.size) 
            throw new AASPException("Position exceeds number of message");

        // we want to turn around message list - newest first
        position = this.size-1 - position; //

        /*
        if(bubbleList != null && position >= cachedFirstIndex && position <= cachedLastIndex) {
            return this.bubbleList.get(position - cachedFirstIndex); // TODO calculation correct?
        }

        // not yet in cache - find chunk
        int cachedFirstIndex = 0;

        // TODO assumed temporal sorted list
        boolean found = false;
        AASPChunk fittingChunk = null;
        for(AASPChunk chunk : this.chunkList) {
            this.cachedLastIndex = this.cachedFirstIndex + chunk.getNumberMessage() - 1;

            if(position >= cachedFirstIndex && position <= cachedLastIndex) {
                fittingChunk = chunk;
                break;
            }

            this.cachedFirstIndex += chunk.getNumberMessage() - 1;
        }

        // chunk found - copy to memory
        this.bubbleList = new ArrayList<>();
        Iterator<CharSequence> messageIter = fittingChunk.getMessages();
        while(messageIter.hasNext()) {
            CharSequence message = messageIter.next();

            BubbleMessageInMemo bubbleMessage = new BubbleMessageInMemo(this.topic, message);
            this.bubbleList.add(bubbleMessage);
        }

        // cache filled - try again
        return this.getMessage(position);
*/
        return "dummy";
    }

    @Override
    public void add(CharSequence message) throws IOException {
        // TODO
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
