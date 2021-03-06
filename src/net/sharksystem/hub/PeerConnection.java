package net.sharksystem.hub;

import java.io.InputStream;
import java.io.OutputStream;

public class PeerConnection {
    /** InputStream to read data from the other side */
    public final InputStream is;
    /** OutputStream to send data to the other side */
    public final OutputStream os;
    /** Name of the peer on the other side */
    public final CharSequence peerID;

    public PeerConnection(CharSequence peerID, InputStream is, OutputStream os) {
        this.peerID = peerID;
        this.is = is;
        this.os = os;
    }
}
