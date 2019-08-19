package net.sharksystem.asap.util;

import java.io.InputStream;
import java.io.OutputStream;
import net.sharksystem.asap.ASAPEngine;
import net.sharksystem.asap.ASAPReceivedChunkListener;

/**
 *
 * @author thsc
 */
public class ASAPEngineThread extends Thread {

    private final ASAPEngine engine;
    private final InputStream is;
    private final OutputStream os;
    private ASAPReceivedChunkListener listener;
    
    public ASAPEngineThread(ASAPEngine engine, InputStream is,
                            OutputStream os, ASAPReceivedChunkListener listener) {
        
        this.engine = engine;
        this.is = is;
        this.os = os;
        this.listener = listener;
    }

    @Override
    public void run() {
        this.engine.handleConnection(this.is, this.os, this.listener);
    }
}
