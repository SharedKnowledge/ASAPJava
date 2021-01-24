package net.sharksystem.asap.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.sharksystem.asap.ASAPConnectionHandler;
import net.sharksystem.asap.ASAPException;

/**
 *
 * @author thsc
 */
public class ASAPPeerHandleConnectionThread extends Thread {
    private final ASAPConnectionHandler engine;
    private final InputStream is;
    private final OutputStream os;

    public ASAPPeerHandleConnectionThread(ASAPConnectionHandler engine, InputStream is, OutputStream os) {
        this.engine = engine;
        this.is = is;
        this.os = os;
    }

    @Override
    public void run() {
        try {
            this.engine.handleConnection(this.is, this.os);
        } catch (IOException | ASAPException e) {
            System.err.println(this.getClass().getSimpleName() + ": " + e.getClass().getSimpleName()
                    + "caught: " + e.getLocalizedMessage());
        }
    }
}
