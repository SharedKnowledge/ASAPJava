package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InterestPDU_Impl extends PDU_Impl implements ASAP_Interest_PDU_1_0 {
    private String sourcePeer;
    private int eraFrom;
    private int eraTo;

    public InterestPDU_Impl(int flagsInt, InputStream is) throws IOException {
        evaluateFlags(flagsInt);

        if(this.peerSet()) { this.readPeer(is); }
        if(this.sourcePeerSet()) { this.readSourcePeer(is); }
        this.readFormat(is);
        if(this.channelSet()) { this.readChannel(is); }
        if(this.eraFromSet()) { this.readFromEra(is); }
        if(this.eraToSet()) { this.readToEra(is); }
    }

    private void readToEra(InputStream is) throws IOException {
        this.eraTo = this.readIntegerParameter(is);
    }

    private void readFromEra(InputStream is) throws IOException {
        this.eraFrom = this.readIntegerParameter(is);
    }

    private void readSourcePeer(InputStream is) throws IOException {
        this.sourcePeer = this.readCharSequenceParameter(is);
    }

    static void sendPDU(CharSequence peer, CharSequence sourcePeer, CharSequence format,
                        CharSequence channel, int eraFrom, int eraTo, OutputStream os, boolean signed)
            throws IOException, ASAPException {

        // first: check protocol errors
        PDU_Impl.checkValidEra(eraFrom);
        PDU_Impl.checkValidEra(eraTo);
        PDU_Impl.checkValidFormat(format);
        PDU_Impl.checkValidSign(peer, signed);
        PDU_Impl.checkValidStream(os);

        // create parameter bytes
        int flags = 0;
        flags = PDU_Impl.setFlag(peer, flags, PEER_BIT_POSITION);
        flags = PDU_Impl.setFlag(sourcePeer, flags, SOURCE_PEER_BIT_POSITION);
        flags = PDU_Impl.setFlag(channel, flags, CHANNEL_BIT_POSITION);
        flags = PDU_Impl.setFlag(eraFrom, flags, ERA_FROM_BIT_POSITION);
        flags = PDU_Impl.setFlag(eraTo, flags, ERA_TO_BIT_POSITION);

        // PDU: CMD | FLAGS | PEER | FORMAT | CHANNEL | ERA
        PDU_Impl.sendCommand(ASAP_1_0.INTEREST_CMD, os); // mand
        PDU_Impl.sendIntegerParameter(flags, os); // mand
        PDU_Impl.sendCharSequenceParameter(peer, os); // opt
        PDU_Impl.sendCharSequenceParameter(sourcePeer, os); // opt
        PDU_Impl.sendCharSequenceParameter(format, os); // mand
        PDU_Impl.sendCharSequenceParameter(channel, os); // opt
        PDU_Impl.sendIntegerParameter(eraFrom, os); // opt
        PDU_Impl.sendIntegerParameter(eraTo, os); // opt

        // TODO: signature
    }

    @Override
    public String getSourcePeer() { return this.sourcePeer;}

    @Override
    public int getEraFrom() { return this.eraFrom; }

    @Override
    public int getEraTo() { return this.eraTo; }
}
