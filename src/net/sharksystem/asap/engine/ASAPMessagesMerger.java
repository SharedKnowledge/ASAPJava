package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAP;
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
        public int position;
        public int sourceIndex;
        public int positionInSource;

        public SourceIndex(int position, int sourceIndex, int positionInSource) {
            this.position = position;
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
                        + " | " + currentFormat);
            }
            // else
            this.format = currentFormat;

            CharSequence currentUri = source.getURI();
            if(this.uri != null && !PeerIDHelper.sameFormat(currentUri, this.uri)) {
                throw new ASAPException("message source must not have different uris: " + this.uri
                        + " | " + currentUri);
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

    private SourceIndex getSourceIndex(int position, List<SourceIndex> indexList) {
        if(indexList.isEmpty()) return null; // not yet initialized?
        SourceIndex previousIndexEntry = null;
        int i = 0;
        do {
            if(indexList.size() <= i) return null;
            SourceIndex indexEntry = indexList.get(i++);

            if(indexEntry.position == position) return indexEntry; // found match

            if(previousIndexEntry != null && previousIndexEntry.position < position && position < indexEntry.position) {
                // we are within a range.
                int offsetIndex = previousIndexEntry.position;
                int steps = position - offsetIndex;
                return new SourceIndex(position,
                        previousIndexEntry.sourceIndex,
                        previousIndexEntry.positionInSource + steps);
            }

            previousIndexEntry = indexEntry;
        } while(previousIndexEntry.position < position); // we have still a chance to find anything

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

    private void setupLookAhead(boolean chronologically) throws IOException, ASAPException {
        byte[][] lookAheadMessagesArray = this.getLookAheadMessages(chronologically);
        SourceIndex[] lookAheadSourceIndexArray = this.getLookAheadSourceIndex(chronologically);

        if(lookAheadMessagesArray[0] != null) return; // already set up.

        // set it up - read first message from each non empty source
        for(int i = 0; i < this.messageSources.length; i++) {
            lookAheadMessagesArray[i] = this.messageSources[i].getMessage(0, chronologically);
            lookAheadSourceIndexArray[i] = new SourceIndex(0, i, 0);
        }
    }

    private SourceIndex lookAhead(int position, List<SourceIndex> indexList, boolean chronologically)
            throws ASAPException, IOException {

        int wantedLookAheadPosition = 0;

        if(!indexList.isEmpty()) { // we already made a look ahead
            // get last entry
            SourceIndex sourceIndex = indexList.get(indexList.size() - 1);

            // check for your own stupidity - last position is before our wanted position - we would not be here otherwise
            if (sourceIndex != null && sourceIndex.position >= position)
                throw new ASAPException("internal error - look ahead algorithm buggy");

            wantedLookAheadPosition = sourceIndex.position+1;
        }

        this.setupLookAhead(chronologically);

        byte[][] lookAheadMessage = this.getLookAheadMessages(chronologically);
        SourceIndex[] lookAheadSourceIndexArray = this.getLookAheadSourceIndex(chronologically);
        //int[] lookAheadMessagePositionInSource = this.getLookAheadPositionInSource(chronologically);

        // find message for next position.
        int bestSourceIndex = 0; // init source index 0 wins?
        SourceIndex previousSourceIndex = null;
        SourceIndex currentSourceIndex = null;

        do {
            for (int i = 1; i < this.messageSources.length; i++) {
                boolean previousEarlier =
                        this.messageCompare.earlier(lookAheadMessage[i - 1], lookAheadMessage[i]);

            /*
            previous earlier | chronologically | what wins?
            -----------------------------------------------
                   true             true           i-1
                   false            true            i
                   true             false           i
                   false            false          i-1
             */

                if ((previousEarlier && chronologically) || (!previousEarlier && !chronologically)) {
                    bestSourceIndex = i - 1;
                } else {
                    bestSourceIndex = 1;
                }
            }

            // we have a winner
            currentSourceIndex = lookAheadSourceIndexArray[bestSourceIndex];

            if(previousSourceIndex != null) {
                if(previousSourceIndex.sourceIndex != currentSourceIndex.sourceIndex) {
                    // remember previous - we have a change in sources
                    indexList.add(previousSourceIndex);
                }
            }

            // prepare next round
            previousSourceIndex = currentSourceIndex;

            // look ahead
            int nextPositionInSource = currentSourceIndex.positionInSource + 1;

            // read ahead
            lookAheadMessage[bestSourceIndex] =
                    messageSources[bestSourceIndex].getMessage(nextPositionInSource, chronologically);

            // remember read ahead
            lookAheadSourceIndexArray[bestSourceIndex] =
                    new SourceIndex(wantedLookAheadPosition, bestSourceIndex, nextPositionInSource);

        } while(position > wantedLookAheadPosition);

        // remember this
        indexList.add(currentSourceIndex);

        return currentSourceIndex;
    }

    @Override
    public byte[] getMessage(int position, boolean chronologically) throws ASAPException, IOException {
        if(position >= this.size)
            throw new ASAPException("position index must not exceed total number of messages: "
                + position + " >= " + this.size);

        SourceIndex sourceIndex = null;
        List<SourceIndex> usedList = chronologically ? oldFirstPositionList : newFirstPositionList;

        sourceIndex = this.getSourceIndex(position, usedList);

        if(sourceIndex == null) {
            // cache miss
            sourceIndex = this.lookAhead(position, usedList, chronologically);
        }
        if(sourceIndex == null) {
            // still nothing
            throw new ASAPException("no message at position: " + position);
        }

        return this.messageSources[sourceIndex.sourceIndex].
                getMessage(sourceIndex.positionInSource, chronologically);
    }
}
