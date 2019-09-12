package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ASAP_Modem_Impl implements ASAP_1_0 {
    // Character are transmitted as bytes: number of bytes (first byte), content following, 0 mean no content

    /*
    general structure:
    CMD | FLAGS | ... specifics
     */

    @Override
    public void offer(CharSequence peer, CharSequence format, CharSequence channel, int era,
                      OutputStream os, boolean signed) throws IOException, ASAPException {

        OfferPDU_Impl.sendPDU(peer, format, channel, era, os, signed);
    }

    @Override
    public void offer(CharSequence peer, CharSequence format, CharSequence channel,
                      OutputStream os, boolean signed) throws IOException, ASAPException {
        this.offer(peer, format, channel, -1, os, signed);
    }

    @Override
    public void interest(CharSequence peer, CharSequence sourcePeer, CharSequence format,
                         CharSequence channel, int eraFrom, int eraTo, OutputStream os, boolean signed)
            throws IOException, ASAPException {

        InterestPDU_Impl.sendPDU(peer, sourcePeer, format, channel, eraFrom, eraTo, os, signed);
    }

    @Override
    public void interest(CharSequence peer, CharSequence sourcePeer, CharSequence format,
                         CharSequence channel, OutputStream os, boolean signed) throws IOException, ASAPException {

        this.interest(peer, sourcePeer, format, channel, -1, -1, os, signed);
    }

    @Override
    public void assimilate(CharSequence peer, CharSequence recipientPeer, CharSequence format,
                           CharSequence channel, int era, long length, List<Long> offsets, InputStream dataIS,
                           OutputStream os, boolean signed) throws IOException, ASAPException {

        AssimilationPDU_Impl.sendPDU(peer, recipientPeer, format, channel, era, length, offsets, dataIS, os, signed);
    }

    @Override
    public void assimilate(CharSequence peer, CharSequence recipientPeer, CharSequence format,
                           CharSequence channel, int era, List<Long> offsets, byte[] data,
                           OutputStream os, boolean signed) throws IOException, ASAPException {

        if(data == null || data.length == 0) throw new ASAPException("data must not be null");

        this.assimilate(peer, recipientPeer, format, channel, era, data.length, offsets,
                new ByteArrayInputStream(data), os, signed);
    }

    @Override
    public ASAP_PDU_1_0 readPDU(InputStream is) throws IOException, ASAPException {
        byte cmd = PDU_Impl.readByte(is);
        int flagsInt = PDU_Impl.readByte(is);

        ASAP_PDU_1_0 pdu = null;

        //System.out.printf("PDU Header cmd(hex): %x | flags(hex): %x\n", cmd, flagsInt);
        switch(cmd) {
            case ASAP_1_0.OFFER_CMD: pdu = new OfferPDU_Impl(flagsInt, is); break;
            case ASAP_1_0.INTEREST_CMD: pdu = new InterestPDU_Impl(flagsInt, is); break;
            case ASAP_1_0.ASSIMILATE_CMD: pdu = new AssimilationPDU_Impl(flagsInt, is); break;
            default: throw new ASAPException("unknown command: " + cmd);
        }
        //System.out.println("got one: " + pdu.toString());

        return pdu;
    }
}
