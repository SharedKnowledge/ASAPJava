package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.engine.ASAPUndecryptableMessageHandler;
import net.sharksystem.asap.crypto.ASAPKeyStore;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static net.sharksystem.asap.protocol.ASAP_1_0.ASAP_MANAGEMENT_FORMAT;

public abstract class ASAPProtocolEngine {
    protected final ASAP_1_0 protocol;
    protected final InputStream is;
    protected final OutputStream os;
    protected final ASAPUndecryptableMessageHandler undecryptableMessageHandler;
    protected final ASAPKeyStore ASAPKeyStore;

    public ASAPProtocolEngine(InputStream is, OutputStream os, ASAP_1_0 protocol,
                              ASAPUndecryptableMessageHandler undecryptableMessageHandler,
                              ASAPKeyStore ASAPKeyStore) {
        this.is = is;
        this.os = os;
        this.protocol = protocol;
        this.undecryptableMessageHandler = undecryptableMessageHandler;
        this.ASAPKeyStore = ASAPKeyStore;

        Log.writeLog(this, "constructor", "is: "
                + is.getClass().getSimpleName() + " | os: " + os.getClass().getSimpleName());
    }

    /**
     * send an interest with nothing but own name / id
     * @param signed if message is signed
     */
    /*
    protected void sendIntroductionOffer(CharSequence owner, boolean signed) throws IOException, ASAPException {
        protocol.offer(owner, ASAP_MANAGEMENT_FORMAT, null, this.os, signed);
    }
     */
}
