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
}
