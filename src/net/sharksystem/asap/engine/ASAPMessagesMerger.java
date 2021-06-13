package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAPChunk;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessageCompare;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.utils.PeerIDHelper;

import javax.xml.transform.Source;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class ASAPMessagesMerger implements ASAPMessages {
    private final ASAPMessageCompare messageCompare;
    private CharSequence format;
    private CharSequence uri;
    private int size;

    private final ASAPMessages[] messageSources;
    private List<SourceIndex> newFirstPositionList = new ArrayList<>();
    private List<SourceIndex> oldFirstPositionList = new ArrayList<>();

    private class SourceIndex {
        public int wantedPosition; // overall position
        public int sourceIndex; // which had fitting message
        public int positionInSource; // position in source where to find message.

        public SourceIndex(int wantedPosition, int sourceIndex, int positionInSource) {
            this.wantedPosition = wantedPosition;
            this.sourceIndex = sourceIndex;
            this.positionInSource = positionInSource;
        }
    }

    ASAPMessagesMerger(List<ASAPMessages> messageSources, ASAPMessageCompare messageCompare)
            throws ASAPException, IOException {
        if(messageSources == null || messageSources.isEmpty())
            throw new ASAPException("message source must not be null or empty");

        this.messageCompare = messageCompare;
        this.size = 0; // init;

        // check integrity and find empty sources
        this.format = null;
        this.uri = null;
        int notEmpty = 0;
        for(ASAPMessages source : messageSources) {
            CharSequence currentFormat = source.getFormat();
            if(this.format != null && !PeerIDHelper.sameFormat(currentFormat, this.format)) {
                throw new ASAPException("message source must not have different formats: " + this.format
                        + " != " + currentFormat);
            }
            // else
            this.format = currentFormat;

            CharSequence currentUri = source.getURI();
            if(this.uri != null && !PeerIDHelper.sameFormat(currentUri, this.uri)) {
                throw new ASAPException("message source must not have different uris: " + this.uri
                        + " != " + currentUri);
            }

            // else
            this.uri = currentUri;
            this.size += source.size();

            if(source.size() > 0) notEmpty++;
        }

        // remember no empty sources
        this.messageSources = new ASAPMessages[notEmpty];
        int i = 0;
        for(ASAPMessages source : messageSources) {
            if(source.size() > 0) this.messageSources[i++] = source;
        }

        if(this.messageCompare == null) {
            // source will be returned in their order
            int globalIndex = 0;
            for(int sIndex = 0; sIndex < this.messageSources.length; sIndex++) {
                SourceIndex index = new SourceIndex(globalIndex, sIndex, 0);
                this.oldFirstPositionList.add(index);
                this.newFirstPositionList.add(index);
                globalIndex += this.messageSources[sIndex].size();
            }
            // last entry
            int lastSourceI = this.messageSources.length-1;
            SourceIndex lastSourceIndex = new SourceIndex(
                    globalIndex-1, // last index
                    lastSourceI, // last source
                    this.messageSources[lastSourceI].size()-1);
            this.oldFirstPositionList.add(lastSourceIndex);
            this.newFirstPositionList.add(lastSourceIndex);
        }
    }

    @Override
    public int size() throws IOException {
        return this.size;
    }

    @Override
    public CharSequence getURI() {
        return this.uri;
    }

    @Override
    public CharSequence getFormat() {
        return this.format;
    }

    @Override
    public Iterator<CharSequence> getMessagesAsCharSequence() throws IOException {
        return new MessageMergerCharSequenceIterator(new MessageMergerIterator(true));
    }

    private class MessageMergerCharSequenceIterator implements Iterator<CharSequence> {
        private final MessageMergerIterator iter;

        MessageMergerCharSequenceIterator(MessageMergerIterator iter) {
            this.iter = iter;
        }

        @Override
        public boolean hasNext() {
            return this.iter.hasNext();
        }

        @Override
        public CharSequence next() {
            return new String(this.iter.next());
        }
    }

    @Override
    public Iterator<byte[]> getMessages() throws IOException {
        return new MessageMergerIterator(true);
    }

    private class MessageMergerIterator implements Iterator<byte[]> {
        private final boolean chronologically;
        private int currentPosition;
        private byte[] lookAheadMessage = null;

        MessageMergerIterator(boolean chronologically) {
            this.chronologically = chronologically;

        }

        @Override
        public boolean hasNext() {
            if(lookAheadMessage == null) {
                // try
                try {
                    this.lookAheadMessage = ASAPMessagesMerger.this.getMessage(currentPosition++, chronologically);
                } catch (ASAPException | IOException e) {
                    return false;
                }
            }
            return (this.lookAheadMessage != null);
        }

        @Override
        public byte[] next() {
            if (this.lookAheadMessage != null) {
                if (this.hasNext()) {
                    byte[] temp = this.lookAheadMessage;
                    this.lookAheadMessage = null;
                    return temp;
                }
            }

            throw new NoSuchElementException("no more messages");
        }
    }

    @Override
    public CharSequence getMessageAsCharSequence(int position, boolean chronologically) throws ASAPException, IOException {
        return new String(this.getMessage(position, chronologically));
    }

    /**
    The list is organized like this: Each entry describes a position of the merged sources: wantedPosition.
    It also describes source (actually the index of the source: sourceIndex) and position of the message
    in the source (positionInSource). There is not necessarily and entry for each wantedPosition - to keep
    that list short.

    A entry is made if the list was empty. There will always be an entry for wantedPosition == 0. Another entry
    will only added if source has changed. Assumed we would have two sources (A and B), first messages come from
    A, follow by B again and so forth, list would look like this: (wantedPosition: 0, A, 0) Both positions will be
    0 in the first entry. Next entry could be: (4, B, 0). Message for position comes from source B index 0. But
    where come message at 1, 2 and 3? From A. There was no change of source, no entry was made. We avoid a series of
    entries like: (1, A, 1), (2, A, 2), (3, A, 3). They can easily be calculated.
     */
    private SourceIndex getSourceIndex(int position, List<SourceIndex> indexList) {
        if(indexList.isEmpty()) return null; // not yet initialized?
        SourceIndex previousIndexEntry = null;
        int i = 0;
        do {
            if(indexList.size() <= i) return null;
            SourceIndex indexEntry = indexList.get(i++);

            if(indexEntry.wantedPosition == position) return indexEntry; // found match

            if(previousIndexEntry != null
                    && previousIndexEntry.wantedPosition < position
                    && position < indexEntry.wantedPosition) {
                // we are within a range.
                int offsetIndex = previousIndexEntry.wantedPosition;
                int steps = position - offsetIndex;
                return new SourceIndex(position,
                        previousIndexEntry.sourceIndex,
                        previousIndexEntry.positionInSource + steps);
            }

            previousIndexEntry = indexEntry;
        } while(previousIndexEntry.wantedPosition < position); // we have still a chance to find anything

        return null; // there is no entry
    }

    private static final int FALSE = 0;
    private static final int TRUE = 1;
    private byte[][][] lookAheadMessages = new byte[2][][]; // actual look ahead message
    private SourceIndex[][] lookAheadSourceIndex = new SourceIndex[2][]; // describes position etc. of previous

    private byte[][] getLookAheadMessages(boolean chronologically) {
        byte[][] a = chronologically ? this.lookAheadMessages[TRUE] : this.lookAheadMessages[FALSE];

        if(a == null) { // not yet initialized
            a  = new byte[this.messageSources.length][];
            if(chronologically) this.lookAheadMessages[TRUE] = a;
            else this.lookAheadMessages[FALSE] = a;
        }

        return a;
    }

    private SourceIndex[] getLookAheadSourceIndex(boolean chronologically) {
        SourceIndex[] s = chronologically ? this.lookAheadSourceIndex[TRUE] : this.lookAheadSourceIndex[FALSE];

        if(s == null) { // not yet initialized
            s  = new SourceIndex[this.messageSources.length];
            if(chronologically) this.lookAheadSourceIndex[TRUE] = s;
            else this.lookAheadSourceIndex[FALSE] = s;
        }

        return s;
    }

    private boolean lookAheadSettedUp = false;
    private SourceIndex lookAhead(int position, List<SourceIndex> indexList, boolean chronologically)
            throws ASAPException, IOException {

        int wantedLookAheadPosition = 0;

        if(!indexList.isEmpty()) { // we already made a look ahead
            // get last entry
            SourceIndex sourceIndex = indexList.get(indexList.size() - 1);

            // check for your own stupidity - last position is before our wanted position - we would not be here otherwise
            if (sourceIndex != null && sourceIndex.wantedPosition >= position)
                throw new ASAPException("internal error - look ahead algorithm buggy");

            wantedLookAheadPosition = sourceIndex.wantedPosition + 1;
        }

        byte[][] lookAheadMessages = this.getLookAheadMessages(chronologically);
        SourceIndex[] lookAheadSourceIndexArray = this.getLookAheadSourceIndex(chronologically);

        if(!this.lookAheadSettedUp) { // setup
            // set it up - read first message from each non empty source
            this.lookAheadSettedUp = true;
            for (int i = 0; i < this.messageSources.length; i++) {
                lookAheadMessages[i] = this.messageSources[i].getMessage(0, chronologically);
                lookAheadSourceIndexArray[i] = new SourceIndex(-1, i, 0);
            }
        }

        // find message for next position.
        SourceIndex previousSourceIndex = null;
        SourceIndex foundSourceIndex = null;

        boolean lastSourceIndexAdded;
        do {
            int bestSourceIndex = -1; // init source index 0 wins?
            lastSourceIndexAdded = false; // not yet at least

            for (int i = 0; i < this.messageSources.length; i++) {
                if(lookAheadMessages[i] == null) continue; // nothing to do in this round
                if(bestSourceIndex == -1) {
                    // guess we have a winner without opponent
                    bestSourceIndex = i;
                    continue;
                }

                boolean previousEarlier =
                        this.messageCompare.earlier(lookAheadMessages[bestSourceIndex], lookAheadMessages[i]);

            /*
            previous earlier | chronologically | what wins?
            -----------------------------------------------
                   true             true           no change
                   false            true            i is better
                   true             false           i is better
                   false            false          no change
             */

                if (previousEarlier != chronologically) { bestSourceIndex = i; }
            }

            if(bestSourceIndex == -1) // failure in algorithm - should not happen due to test of size()..
                throw new ASAPException("no more message - cannot look ahead");

            // we have a winner
            foundSourceIndex = lookAheadSourceIndexArray[bestSourceIndex];

            // now we can tell at what overall position this message is
            foundSourceIndex.wantedPosition = wantedLookAheadPosition;

            if(previousSourceIndex != null) {
                if(previousSourceIndex.sourceIndex != foundSourceIndex.sourceIndex) {
                    // remember previous - we have a change in sources
                    indexList.add(foundSourceIndex);
                    lastSourceIndexAdded = true;
                }
            } else {
                indexList.add(foundSourceIndex);
                lastSourceIndexAdded = true;
            }

            // prepare next round
            previousSourceIndex = foundSourceIndex;

            // read ahead - if possible
            lookAheadSourceIndexArray[bestSourceIndex] = null;
            lookAheadMessages[bestSourceIndex] = null;
            int nextPositionInSource = foundSourceIndex.positionInSource + 1;
            try {
                lookAheadMessages[bestSourceIndex] =
                        messageSources[bestSourceIndex].getMessage(nextPositionInSource, chronologically);

                // remember read ahead
                lookAheadSourceIndexArray[bestSourceIndex] =
                        new SourceIndex(-1, // we cannot tell yet
                                bestSourceIndex, nextPositionInSource); // we know where it comes from
            }
            catch(Exception e) {
                // no more messages
            }
        } while(position > wantedLookAheadPosition++);

        // remember this in any case
        if(!lastSourceIndexAdded) indexList.add(foundSourceIndex);

        return foundSourceIndex;
    }

    private List<SourceIndex> getIndexList(boolean chronologically) {
        return chronologically ? oldFirstPositionList : newFirstPositionList;
    }

    private SourceIndex getSourceIndex(int position, boolean chronologically) throws ASAPException, IOException {
        if(position >= this.size)
            throw new ASAPException("position index must not exceed total number of messages: "
                    + position + " >= " + this.size);

        SourceIndex sourceIndex;
        List<SourceIndex> usedList = this.getIndexList(chronologically);

        sourceIndex = this.getSourceIndex(position, usedList);

        if(sourceIndex == null) {
            // cache miss
            sourceIndex = this.lookAhead(position, usedList, chronologically);
        }
        if(sourceIndex == null) {
            // still nothing
            throw new ASAPException("no message at position: " + position);
        }

        return sourceIndex;
    }

    @Override
    public byte[] getMessage(int position, boolean chronologically) throws ASAPException, IOException {
        SourceIndex sourceIndex = this.getSourceIndex(position, chronologically);
        return this.messageSources[sourceIndex.sourceIndex].getMessage(
                sourceIndex.positionInSource, chronologically);
    }

    @Override
    public ASAPChunk getChunk(int position, boolean chronologically) throws IOException, ASAPException {
        SourceIndex sourceIndex = this.getSourceIndex(position, chronologically);
        return this.messageSources[sourceIndex.sourceIndex].getChunk(sourceIndex.positionInSource, chronologically);
    }
}
