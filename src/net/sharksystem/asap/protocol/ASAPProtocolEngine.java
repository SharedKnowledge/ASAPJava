package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static net.sharksystem.asap.protocol.ASAP_1_0.ASAP_MANAGEMENT_FORMAT;

public abstract class ASAPProtocolEngine {
    protected final ASAP_1_0 protocol;
    protected final InputStream is;
    protected final OutputStream os;

    public ASAPProtocolEngine(InputStream is, OutputStream os, ASAP_1_0 protocol) {
        this.is = is;
        this.os = os;
        this.protocol = protocol;
    }

    /**
     * send an interest with nothing but own name / id
     * @param signed if message is signed
     */
    protected void sendIntroductionOffer(CharSequence owner, boolean signed) throws IOException, ASAPException {
        protocol.offer(owner, ASAP_MANAGEMENT_FORMAT, null, this.os, signed);
    }
}
