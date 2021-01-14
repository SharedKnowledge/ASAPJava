package net.sharksystem.asap.internals;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.protocol.ASAPConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ASAPConnectionHandler {
    ASAPConnection handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException;
}
