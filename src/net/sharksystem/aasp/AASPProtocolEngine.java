package net.sharksystem.aasp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author thsc
 */
interface AASPProtocolEngine {
    public void handleConnection(InputStream is, OutputStream os,
            AASPReceivedChunkListener listener) throws IOException;

    /**
     * Chunks are (tried to be) delivered to their recipients during each encounter
     * with another peer. After successful delivery, recipient is withdrawn from recipient
     * list to prevent mutliple delivery.
     *
     * If this flag is set, chunk are removed permanently if their are delivered
     * to all their recipients. If no set, they remain intact.
     * @param drop
     */
    void setDropDeliveredChunks(boolean drop);
}
