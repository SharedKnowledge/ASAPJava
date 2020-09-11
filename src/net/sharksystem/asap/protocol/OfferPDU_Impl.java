package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.utils.ASAPSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class OfferPDU_Impl extends PDU_Impl implements ASAP_OfferPDU_1_0 {

    public OfferPDU_Impl(int flagsInt, boolean encrypted, InputStream is) throws IOException, ASAPException {
        super(ASAP_1_0.OFFER_CMD, encrypted);
        evaluateFlags(flagsInt);

        if(this.senderSet()) { this.readSender(is); }
        this.readFormat(is);
        if(this.channelSet()) { this.readChannel(is); }
        if(this.eraSet()) { this.readEra(is); }
    }

    static void sendPDU(CharSequence peer, CharSequence format, CharSequence channel, int era,
                        OutputStream os, boolean signed) throws IOException, ASAPException {

        // first: check protocol errors
        PDU_Impl.checkValidEra(era);
        PDU_Impl.checkValidFormat(format);
//        PDU_Impl.checkValidSign(peer, signed);
        PDU_Impl.checkValidStream(os);

        // create parameter bytes
        int flags = 0;
        flags = PDU_Impl.setFlag(peer, flags, SENDER_BIT_POSITION);
        flags = PDU_Impl.setFlag(channel, flags, CHANNEL_BIT_POSITION);
        flags = PDU_Impl.setFlag(era, flags, ERA_BIT_POSITION);

        // PDU: CMD | FLAGS | PEER | FORMAT | CHANNEL | ERA
        PDU_Impl.sendHeader(ASAP_1_0.OFFER_CMD, flags, os);

        ASAPSerialization.writeCharSequenceParameter(peer, os); // opt
        ASAPSerialization.writeCharSequenceParameter(format, os); // mand
        ASAPSerialization.writeCharSequenceParameter(channel, os); // opt
        ASAPSerialization.writeNonNegativeIntegerParameter(era, os); // opt
    }
}
