package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class OfferPDU_Impl extends PDU_Impl {
    public OfferPDU_Impl(int flagsInt, InputStream is) throws IOException {
        evaluateFlags(flagsInt);

        if(this.peerSet()) { this.readPeer(is); }
        this.readFormat(is);
        if(this.channelSet()) { this.readChannel(is); }
        if(this.eraSet()) { this.readEra(is); }
    }

    static void sendPDU(CharSequence peer, CharSequence format, CharSequence channel, int era,
                        OutputStream os, boolean signed) throws IOException, ASAPException {

        // first: check protocol errors
        PDU_Impl.checkValidEra(era);
        PDU_Impl.checkValidFormat(format);
        PDU_Impl.checkValidSign(peer, signed);
        PDU_Impl.checkValidStream(os);

        // create parameter bytes
        int flags = 0;
        flags = PDU_Impl.setFlag(peer, flags, PEER_BIT_POSITION);
        flags = PDU_Impl.setFlag(channel, flags, CHANNEL_BIT_POSITION);
        flags = PDU_Impl.setFlag(era, flags, ERA_BIT_POSITION);

        // PDU: CMD | FLAGS | PEER | FORMAT | CHANNEL | ERA
        PDU_Impl.sendCommand(ASAP_1_0.OFFER_CMD, os); // mand
        PDU_Impl.sendIntegerParameter(flags, os); // mand
        PDU_Impl.sendCharSequenceParameter(peer, os); // opt
        PDU_Impl.sendCharSequenceParameter(format, os); // mand
        PDU_Impl.sendCharSequenceParameter(channel, os); // opt
        PDU_Impl.sendIntegerParameter(era, os); // opt

        // TODO: signature
    }
}
