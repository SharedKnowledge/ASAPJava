package net.sharksystem.asap;

import net.sharksystem.asap.engine.ASAPChunkReceivedListener;

public interface ASAPPeerService extends ASAPPeer, ASAPConnectionHandler {
    //    long DEFAULT_MAX_PROCESSING_TIME = 30000; // 30 seconds
    long DEFAULT_MAX_PROCESSING_TIME = Long.MAX_VALUE; // eternity - debugging setting

    /**
     * Overwrite internal listener. This method is used e.g. in Androiud on service side. The asap peer is informed
     * about newly received chunks. That news is broadcasted to application side which performs the actual processing.
     *
     * @param listener this listener is called instead - no further chunk processing is made by this object.
     */
    void overwriteChuckReceivedListener(ASAPChunkReceivedListener listener);
}
