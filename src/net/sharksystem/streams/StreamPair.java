package net.sharksystem.streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface StreamPair {
    /** InputStream to read data from the other side */
    InputStream getInputStream() throws IOException;
    /** OutputStream to send data to the other side */
    OutputStream getOutputStream() throws IOException;
    /** close connection to peer */
    void close();

    void addListener(StreamPairListener listener);

    CharSequence getSessionID();

    CharSequence getEndpointAddress();
}
