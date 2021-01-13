package net.sharksystem.asap;

import net.sharksystem.asap.internals.ASAPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ASAPPeerService extends ASAPPeer {
    void handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException;
}
