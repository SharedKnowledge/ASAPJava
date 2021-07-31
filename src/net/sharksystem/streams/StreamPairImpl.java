package net.sharksystem.streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamPairImpl extends StreamPairListenerManager implements StreamPair {
    private final InputStream is;
    private final OutputStream os;
    private final CharSequence sessionID;
    private CharSequence endpointAddress;

    public static final String NO_ID = "no ID";

    public static StreamPair getStreamPair(InputStream is, OutputStream os) {
        return new StreamPairImpl(is, os, NO_ID, NO_ID);
    }

    public static StreamPair getStreamPair(InputStream is, OutputStream os,
                           CharSequence sessionID, CharSequence endpointAddress) {
        return new StreamPairImpl(is, os, sessionID, endpointAddress);
    }

    public static StreamPair getStreamPairWithSessionID(InputStream is, OutputStream os, CharSequence sessionID) {
        return new StreamPairImpl(is, os, sessionID, NO_ID);
    }

    public static StreamPair getStreamPairWithEndpointAddress(InputStream is, OutputStream os,
                                                              CharSequence endpointAddress) {
        return new StreamPairImpl(is, os, NO_ID, endpointAddress);
    }

    /**
     * Create a pair of in- and output streams (most probably a kind of socket) with an ID. This ID should be
     * an endpoint address, e.g. a mac address, IP/TCP - address. It could also be an ASAP peer ID.
     * @param is
     * @param os
     * @param sessionID a session id
     */
    private StreamPairImpl(InputStream is, OutputStream os, CharSequence sessionID, CharSequence endpointAddress) {
        this.sessionID = sessionID;
        this.endpointAddress = endpointAddress;
        this.is = is;
        this.os = os;
    }

    @Override
    public InputStream getInputStream() { return this.is; }

    @Override
    public OutputStream getOutputStream() { return this.os; }

    public CharSequence getSessionID() {
        return this.sessionID;
    }

    public CharSequence getEndpointAddress() {
        return this.endpointAddress;
    }

    @Override
    public void close() {
        try {
            is.close();
        } catch (IOException e) {
            // ignore
        }
        try {
            os.close();
        } catch (IOException e) {
            // ignore
        }

        this.notifyAllListenerClosed(this, this.sessionID.toString());
    }

    public String toString() {
        return "StreamPair (" + this.sessionID + ")";
    }
}
