package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class AssimilationPDU_Impl extends PDU_Impl implements ASAP_AssimilationPDU_1_0 {
    static void sendPDU(CharSequence peer, CharSequence recipientPeer, CharSequence channel, int era,
                        CharSequence format, int length, InputStream is, OutputStream os, boolean signed)
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
    public boolean recipientPeerSet() {
        return false;
    }

    @Override
    public String getRecipientPeer() {
        return null;
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    @Override
    public void streamData(OutputStream os) throws IOException {

    }
}
