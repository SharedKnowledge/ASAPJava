package net.sharksystem.asap.protocol;

import net.sharksystem.asap.engine.ASAPUndecryptableMessageHandler;
import net.sharksystem.asap.crypto.ASAPKeyStore;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ASAPProtocolEngine {
    protected final ASAP_1_0 protocol;
    protected final InputStream is;
    protected final OutputStream os;
    protected final ASAPUndecryptableMessageHandler undecryptableMessageHandler;
    protected final ASAPKeyStore ASAPKeyStore;

    public ASAPProtocolEngine(InputStream is, OutputStream os, ASAP_1_0 protocol,
                              ASAPUndecryptableMessageHandler undecryptableMessageHandler,
                              ASAPKeyStore ASAPKeyStore) {
        /*
        this.is = new ISWrapper(is);
        this.os = new OSWrapper(os);
         */
        this.is = is;
        this.os = os;
        this.protocol = protocol;
        this.undecryptableMessageHandler = undecryptableMessageHandler;
        this.ASAPKeyStore = ASAPKeyStore;

        Log.writeLog(this, "constructor", "is: "
                + is.getClass().getSimpleName() + " | os: " + os.getClass().getSimpleName());
    }

    private class ISWrapper extends InputStream {
        private final InputStream is;

        ISWrapper(InputStream is) {
            this.is = is;
        }
        @Override
        public int read() throws IOException {
            return is.read();
        }
        public void close() {
            Log.writeLog(this, "wrapper: close called");
        }
    }

    private class OSWrapper extends OutputStream {
        private final OutputStream os;

        OSWrapper(OutputStream is) {
            this.os = is;
        }

        @Override
        public void write(int b) throws IOException {
            this.os.write(b);
        }

        public void close() {
            Log.writeLog(this, "wrapper: close called");
        }
    }
}
