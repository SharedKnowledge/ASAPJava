package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.InputStream;
import java.io.OutputStream;

public interface ASAPStartupConnection {
    /**
     * @return true if Streams are still open
     */
    boolean streamsOpen();

    /**
     * @return id of connected peer
     * @exception ASAPException if no peer id available
     */
    CharSequence getPeerID() throws ASAPException;

    /**
     *
     * @return open input stream
     * @throws ASAPException if stream is no longer open
     */
    InputStream getInputStream() throws ASAPException;

    /**
     *
     * @return open output stream
     * @throws ASAPException if stream is no longer open
     */
    OutputStream getOutputStream() throws ASAPException;
}
