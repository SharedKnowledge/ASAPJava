package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class AssimilationPDU_Impl extends PDU_Impl implements ASAP_AssimilationPDU_1_0 {
    private final int dataLength;
    private final InputStream is;
    private String recipientPeer;

    public AssimilationPDU_Impl(int flagsInt, InputStream is) throws IOException {
        evaluateFlags(flagsInt);

        if(this.peerSet()) { this.readPeer(is); }
        if(this.recipientPeerSet()) { this.readRecipientPeer(is); }
        this.readFormat(is);
        if(this.channelSet()) { this.readChannel(is); }
        if(this.eraSet()) { this.readEra(is); }

        this.dataLength = is.read();

        this.is = is;
    }

    private void readRecipientPeer(InputStream is) throws IOException {
        this.recipientPeer = this.readCharSequenceParameter(is);
    }

    static void sendPDU(CharSequence peer, CharSequence recipientPeer, CharSequence format, CharSequence channel,
                        int era, int length, InputStream is, OutputStream os, boolean signed)
            throws IOException, ASAPException {

        // first: check protocol errors
        PDU_Impl.checkValidEra(era);
        PDU_Impl.checkValidFormat(format);
        PDU_Impl.checkValidSign(peer, signed);
        PDU_Impl.checkValidStream(os);

        // create parameter bytes
        int flags = 0;
        flags = PDU_Impl.setFlag(peer, flags, PEER_BIT_POSITION);
        flags = PDU_Impl.setFlag(recipientPeer, flags, RECIPIENT_PEER_BIT_POSITION);
        flags = PDU_Impl.setFlag(channel, flags, CHANNEL_BIT_POSITION);
        flags = PDU_Impl.setFlag(era, flags, ERA_BIT_POSITION);

        // PDU: CMD | FLAGS | PEER | FORMAT | CHANNEL | ERA
        PDU_Impl.sendCommand(ASAP_1_0.ASSIMILATE_CMD, os); // mand
        PDU_Impl.sendIntegerParameter(flags, os); // mand
        PDU_Impl.sendCharSequenceParameter(peer, os); // opt
        PDU_Impl.sendCharSequenceParameter(recipientPeer, os); // opt
        PDU_Impl.sendCharSequenceParameter(format, os); // mand
        PDU_Impl.sendCharSequenceParameter(channel, os); // opt
        PDU_Impl.sendIntegerParameter(era, os); // opt
        PDU_Impl.sendIntegerParameter(length, os); // mand

        // stream data
        while(length-- > 0) {
            os.write(is.read());
        }

        // TODO: signature
    }

    @Override
    public String getRecipientPeer() { return this.recipientPeer;  }

    @Override
    public int getLength() { return this.dataLength; }

    @Override
    public byte[] getData() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.streamData(baos);

        return baos.toByteArray();
    }

    @Override
    public void streamData(OutputStream os) throws IOException {
        for(int i = 0; i < this.dataLength; i++) {
            os.write(this.is.read());
        }
    }
}
