package net.sharksystem.asap;

import net.sharksystem.asap.internals.ASAPChunkReceivedListener;
import net.sharksystem.asap.internals.ASAPInternalPeer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ASAPPeerService extends ASAPPeer {
    long DEFAULT_MAX_PROCESSING_TIME = ASAPInternalPeer.DEFAULT_MAX_PROCESSING_TIME;

    void handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException;

    /**
     * Overwrite internal listener. This method is used e.g. in Androiud on service side. The asap peer is informed
     * about newly received chunks. That news is broadcasted to application side which performs the actual processing.
     *
     * @param listener this listener is called instead - no further chunk processing is made by this object.
     */
    void overwriteChuckReceivedListener(ASAPChunkReceivedListener listener);
}
