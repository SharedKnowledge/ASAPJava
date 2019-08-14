package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;

public class PDUTests {

    @Test
    public void sendAndReceiveOffer() throws IOException, ASAPException {
        ASAP_1_0 protocolEngine = new ASAP_Modem_Impl();

        String peer = "Alice";
        String channel = "AliceURI";
        String format = "format";
        int era = 1;

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        /*
            void offer(CharSequence peer, CharSequence format, CharSequence channel, int era, OutputStream os, boolean signed)
            throws IOException, ASAPException;
         */

        protocolEngine.offer(peer, format, channel, era, os, false);

        // try t read output
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        ASAP_PDU_1_0 asap_pdu_1_0 = protocolEngine.readPDU(is);

        ASAP_OfferPDU_1_0 offerPDU = (ASAP_OfferPDU_1_0) asap_pdu_1_0;

        Assert.assertTrue(offerPDU.getChannel().equalsIgnoreCase(channel));
        Assert.assertTrue(offerPDU.getFormat().equalsIgnoreCase(format));
        Assert.assertTrue(offerPDU.getPeer().equalsIgnoreCase(peer));
        Assert.assertEquals(offerPDU.getEra(), era);
    }

    @Test
    public void sendAndReceiveInterest() throws IOException, ASAPException {
        ASAP_1_0 protocolEngine = new ASAP_Modem_Impl();

        String peer = "Alice";
        String sourcePeer = "Bob";
        String channel = "AliceURI";
        String format = "format";
        int eraFrom = 1;
        int eraTo = 2;

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        /*
    void interest(CharSequence peer, CharSequence sourcePeer, CharSequence format,
                  CharSequence channel, int eraFrom, int eraTo,
                  OutputStream os, boolean signed) throws IOException, ASAPException;
         */

        protocolEngine.interest(peer, sourcePeer, format, channel, eraFrom, eraTo, os, false);

        // try t read output
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        ASAP_PDU_1_0 asap_pdu_1_0 = protocolEngine.readPDU(is);

        ASAP_Interest_PDU_1_0 interestPDU = (ASAP_Interest_PDU_1_0) asap_pdu_1_0;

        Assert.assertTrue(interestPDU.getChannel().equalsIgnoreCase(channel));
        Assert.assertTrue(interestPDU.getFormat().equalsIgnoreCase(format));
        Assert.assertTrue(interestPDU.getPeer().equalsIgnoreCase(peer));
        Assert.assertTrue(interestPDU.getSourcePeer().equalsIgnoreCase(sourcePeer));
        Assert.assertEquals(interestPDU.getEraFrom(), eraFrom);
        Assert.assertEquals(interestPDU.getEraTo(), eraTo);
    }

    @Test
    public void sendAndReceiveAssimilate() throws IOException, ASAPException {
        ASAP_1_0 protocolEngine = new ASAP_Modem_Impl();

        String peer = "Alice";
        String recipientPeer = "Bob";
        String channel = "AliceURI";
        String format = "format";
        int era = 1;
        String testString = "test data";
        byte[] data = testString.getBytes();

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        /*
    void assimilate(CharSequence peer, CharSequence recipientPeer, CharSequence format, CharSequence channel, int era,
                    byte[] data, OutputStream os, boolean signed) throws IOException, ASAPException;
         */

        protocolEngine.assimilate(peer, recipientPeer, format, channel, era, data, os,false);

        // try t read output
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        ASAP_PDU_1_0 asap_pdu_1_0 = protocolEngine.readPDU(is);

        ASAP_AssimilationPDU_1_0 assimilationPDU = (ASAP_AssimilationPDU_1_0) asap_pdu_1_0;

        Assert.assertTrue(assimilationPDU.getChannel().equalsIgnoreCase(channel));
        Assert.assertTrue(assimilationPDU.getFormat().equalsIgnoreCase(format));
        Assert.assertTrue(assimilationPDU.getPeer().equalsIgnoreCase(peer));
        Assert.assertTrue(assimilationPDU.getRecipientPeer().equalsIgnoreCase(recipientPeer));
        Assert.assertEquals(assimilationPDU.getEra(), era);

        byte[] data1 = assimilationPDU.getData();

        Assert.assertTrue(new String(data1).equalsIgnoreCase(testString));
    }
}
