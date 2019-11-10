package net.sharksystem.asap.apps;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ASAPHandleConnectionThread extends Thread {
    private final InputStream is;
    private final OutputStream os;
    private final ASAPJavaApplication asapJavaApp;

    public ASAPHandleConnectionThread(ASAPJavaApplication asapJavaApp, InputStream is, OutputStream os) {
            this.asapJavaApp = asapJavaApp;
            this.is = is;
            this.os = os;
    }

    @Override
    public void run() {
        try {
            this.asapJavaApp.handleConnection(this.is, this.os);
        } catch (IOException | ASAPException e) {
            System.err.println(this.getClass().getSimpleName() + ": " + e.getClass().getSimpleName()
                    + "caught: " + e.getLocalizedMessage());
        }
    }
}
