package net.sharksystem.asap.engine;

import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class MessageIter implements Iterator {
    private final List<byte[]> byteMessages;
    private int nextIndex;
    private String nextString;


    public MessageIter(List<byte[]> byteMessages) throws FileNotFoundException {
        this.byteMessages = byteMessages;
        this.nextIndex = 0;
    }

    @Override
    public boolean hasNext() {
        return this.byteMessages.size() > nextIndex;
    }

    @Override
    public String next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException("no more messages");
        }

        return new String(this.byteMessages.get(nextIndex++));
    }
}
