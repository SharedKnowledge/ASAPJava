package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.io.InputStream;

public class ASAPPDUReader extends Thread {
    private final ASAP_1_0 protocol;
    private final InputStream is;
    private final ThreadFinishedListener pduReaderListener;
    private ASAP_PDU_1_0 asapPDU = null;
    private IOException ioException = null;
    private ASAPException asapException = null;

    ASAPPDUReader(ASAP_1_0 protocol, InputStream is, ThreadFinishedListener listener) {
        this.protocol = protocol;
        this.is = is;
        this.pduReaderListener = listener;
    }

    IOException getIoException() {
        return this.ioException;
    }

    ASAPException getAsapException() {
        return this.asapException;
    }

    ASAP_PDU_1_0 getASAPPDU() {
        return this.asapPDU;
    }

    public void run() {
        try {
            this.asapPDU = protocol.readPDU(is);
            this.pduReaderListener.finished(this);
        } catch (IOException e) {
            this.ioException = e;
        } catch (ASAPException e) {
            this.asapException = e;
        }
    }
}
