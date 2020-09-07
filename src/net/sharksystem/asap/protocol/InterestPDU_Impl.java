package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class InterestPDU_Impl extends PDU_Impl implements ASAP_Interest_PDU_1_0 {
    private int eraFrom;
    private int eraTo;

    InterestPDU_Impl(int flagsInt, boolean encrypted, InputStream is) throws IOException, ASAPException {
        super(ASAP_1_0.INTEREST_CMD, encrypted);

        evaluateFlags(flagsInt);

        if(this.senderSet()) { this.readSender(is); }
        if(this.recipientSet()) { this.readRecipient(is); }
        this.readFormat(is); // mandatory
        if(this.channelSet()) { this.readChannel(is); }
        if(this.eraFromSet()) { this.readFromEra(is); }
        if(this.eraToSet()) { this.readToEra(is); }
    }

    private void readToEra(InputStream is) throws IOException, ASAPException {
        this.eraTo = this.readIntegerParameter(is);
    }

    private void readFromEra(InputStream is) throws IOException, ASAPException {
        this.eraFrom = this.readIntegerParameter(is);
    }

    static void sendPDUWithoutCmd(CharSequence sender, CharSequence recipient, CharSequence format,
                                  CharSequence channel, int eraFrom, int eraTo, OutputStream os,
                                  boolean signed)
            throws IOException, ASAPException {

        if(format == null || format.length() < 1) format = ASAP_1_0.ANY_FORMAT;

        // first: check protocol errors
        PDU_Impl.checkValidEra(eraFrom);
        PDU_Impl.checkValidEra(eraTo);
        PDU_Impl.checkValidFormat(format);
        PDU_Impl.checkValidStream(os);

        // create parameter bytes
        int flags = 0;
        flags = PDU_Impl.setFlag(sender, flags, SENDER_BIT_POSITION);
        flags = PDU_Impl.setFlag(recipient, flags, RECIPIENT_BIT_POSITION);
        flags = PDU_Impl.setFlag(channel, flags, CHANNEL_BIT_POSITION);
        flags = PDU_Impl.setFlag(eraFrom, flags, ERA_FROM_BIT_POSITION);
        flags = PDU_Impl.setFlag(eraTo, flags, ERA_TO_BIT_POSITION);
        flags = PDU_Impl.setFlag(signed, flags, SIGNED_TO_BIT_POSITION);

        PDU_Impl.sendFlags(flags, os);

        PDU_Impl.sendCharSequenceParameter(sender, os); // opt
        PDU_Impl.sendCharSequenceParameter(recipient, os); // opt
        PDU_Impl.sendCharSequenceParameter(format, os); // mand
        PDU_Impl.sendCharSequenceParameter(channel, os); // opt
        PDU_Impl.sendNonNegativeIntegerParameter(eraFrom, os); // opt
        PDU_Impl.sendNonNegativeIntegerParameter(eraTo, os); // opt
    }

    @Override
    public int getEraFrom() { return this.eraFrom; }

    @Override
    public int getEraTo() { return this.eraTo; }
}
