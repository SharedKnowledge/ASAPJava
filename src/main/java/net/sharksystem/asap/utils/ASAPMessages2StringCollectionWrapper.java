package net.sharksystem.asap.utils;

import net.sharksystem.asap.ASAPMessages;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Assumed ASAPMessages only contain a string - instantiate an object of this class
 * with ASAPMessages and get a String iterator
 */
public class ASAPMessages2StringCollectionWrapper implements Iterator<String> {
    private final ASAPMessages asapMessages;
    private final Iterator<byte[]> byteContentInterator;

    public ASAPMessages2StringCollectionWrapper(ASAPMessages asapMessages) throws IOException {
        this.asapMessages = asapMessages;
        this.byteContentInterator = this.asapMessages.getMessages();
    }

    @Override
    public boolean hasNext() {
        return this.byteContentInterator.hasNext();
    }

    @Override
    public String next() {
        return new String(this.byteContentInterator.next());
    }
}
