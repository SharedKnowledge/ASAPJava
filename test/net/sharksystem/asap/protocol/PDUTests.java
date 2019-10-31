package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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

        Assert.assertTrue(offerPDU.getChannelUri().equalsIgnoreCase(channel));
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

        Assert.assertTrue(interestPDU.getChannelUri().equalsIgnoreCase(channel));
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

        String testString1 = "data1";
        String testString2 = "data2 longer";
        List<Long> offsetsList = new ArrayList<Long>();

        byte[] testData1 = testString1.getBytes();
        byte[] testData2 = testString2.getBytes();
        long len = testData1.length;
        offsetsList.add(len);

        ByteArrayOutputStream testDataOutputStream = new ByteArrayOutputStream();

        testDataOutputStream.write(testData1);
        testDataOutputStream.write(testData2);

        byte[] data = testDataOutputStream.toByteArray();

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        protocolEngine.assimilate(peer, recipientPeer, format, channel, era, offsetsList, data, os,false);

        // try t read output
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        ASAP_PDU_1_0 asap_pdu_1_0 = protocolEngine.readPDU(is);

        ASAP_AssimilationPDU_1_0 assimilationPDU = (ASAP_AssimilationPDU_1_0) asap_pdu_1_0;

        Assert.assertTrue(assimilationPDU.getChannelUri().equalsIgnoreCase(channel));
        Assert.assertTrue(assimilationPDU.getFormat().equalsIgnoreCase(format));
        Assert.assertTrue(assimilationPDU.getPeer().equalsIgnoreCase(peer));
        Assert.assertTrue(assimilationPDU.getRecipientPeer().equalsIgnoreCase(recipientPeer));
        Assert.assertEquals(assimilationPDU.getEra(), era);

        byte[] data_received = assimilationPDU.getData();
        List<Integer> offsets_received = assimilationPDU.getMessageOffsets();
        // one entry assumed
        int offset = offsets_received.get(0);

        byte[] data_r1 = new byte[offset];
        for(int i = 0; i < offset; i++) {
            data_r1[i] = data_received[i];
        }

        int remainingByteNumber = data_received.length-offset;
        byte[] data_r2 = new byte[remainingByteNumber];
        for(int i = 0; i < remainingByteNumber; i++) {
            data_r2[i] = data_received[i+offset];
        }

        Assert.assertTrue(new String(data_r1).equalsIgnoreCase(testString1));
        Assert.assertTrue(new String(data_r2).equalsIgnoreCase(testString2));
    }

    // protocol.interest(this.owner, null, null, null, -1, -1, os, false);
    @Test
    public void sendAndReceiveInterest2() throws IOException, ASAPException {
        ASAP_1_0 protocolEngine = new ASAP_Modem_Impl();

        String peer = "Alice";
        String sourcePeer = null;
        String channel = null;
        String format = null;
        int eraFrom = -1;
        int eraTo = -1;

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

        Assert.assertFalse(interestPDU.channelSet());
        Assert.assertTrue(interestPDU.getFormat().equalsIgnoreCase(ASAP_1_0.ANY_FORMAT.toString()));
        Assert.assertTrue(interestPDU.getPeer().equalsIgnoreCase(peer));
        Assert.assertFalse(interestPDU.sourcePeerSet());
        Assert.assertFalse(interestPDU.eraFromSet());
        Assert.assertFalse(interestPDU.eraToSet());
    }
}
