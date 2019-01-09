package net.sharksystem.asp3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author thsc
 */
interface ASP3ProtocolEngine {
    public void handleConnection(InputStream is, OutputStream os) 
            throws IOException;
}
