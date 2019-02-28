package net.sharksystem.aasp.util;

import java.io.InputStream;
import java.io.OutputStream;
import net.sharksystem.aasp.AASPEngine;
import net.sharksystem.aasp.AASPReceivedChunkListener;

/**
 *
 * @author thsc
 */
public class AASPEngineThread extends Thread {

    private final AASPEngine engine;
    private final InputStream is;
    private final OutputStream os;
    private AASPReceivedChunkListener listener;
    
    public AASPEngineThread(AASPEngine engine, InputStream is,
            OutputStream os, AASPReceivedChunkListener listener) {
        
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
