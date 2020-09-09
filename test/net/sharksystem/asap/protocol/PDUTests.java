package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.crypto.BasicCryptoKeyStorage;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

public class PDUTests {
    public static final String ALICE_ID = "Alice";
    public static final String BOB_ID = "Bob";
    public static final String CLARA_ID = "Clara";

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
        Assert.assertTrue(offerPDU.getSender().equalsIgnoreCase(peer));
        Assert.assertEquals(offerPDU.getEra(), era);
    }

    ////////////////////           interest          /////////////////////////////////////////
    @Test
    public void sendAndReceiveInterest() throws IOException, ASAPException {
        ASAP_1_0 protocolEngine = new ASAP_Modem_Impl();

        String sender = "Alice";
        String recipient = "Bob";
        String channel = "AliceURI";
        String format = "format";
        int eraFrom = 1;
        int eraTo = 2;

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        protocolEngine.interest(sender, recipient, format, channel, eraFrom, eraTo, os, false);

        // try t read output
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        ASAP_PDU_1_0 asap_pdu_1_0 = protocolEngine.readPDU(is);

        ASAP_Interest_PDU_1_0 interestPDU = (ASAP_Interest_PDU_1_0) asap_pdu_1_0;

        Assert.assertTrue(interestPDU.getChannelUri().equalsIgnoreCase(channel));
        Assert.assertTrue(interestPDU.getFormat().equalsIgnoreCase(format));
        Assert.assertTrue(interestPDU.getSender().equalsIgnoreCase(sender));
        Assert.assertTrue(interestPDU.getRecipient().equalsIgnoreCase(recipient));
        Assert.assertEquals(interestPDU.getEraFrom(), eraFrom);
        Assert.assertEquals(interestPDU.getEraTo(), eraTo);
    }

    // protocol.interest(this.owner, null, null, null, -1, -1, os, false);
    @Test
    public void sendAndReceiveInterest2() throws IOException, ASAPException {
        ASAP_1_0 protocolEngine = new ASAP_Modem_Impl();

        String sender = "Alice";
        String recipient = null;
        String channel = null;
        String format = null; // will be corrected
        /*
        int eraFrom = ERA_NOT_DEFINED;
        int eraTo = ERA_NOT_DEFINED;
         */

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        protocolEngine.interest(sender, recipient, format, channel, os);

        // try t read output
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        ASAP_PDU_1_0 asap_pdu_1_0 = protocolEngine.readPDU(is);

        ASAP_Interest_PDU_1_0 interestPDU = (ASAP_Interest_PDU_1_0) asap_pdu_1_0;

        Assert.assertFalse(interestPDU.channelSet());
        Assert.assertTrue(interestPDU.getFormat().equalsIgnoreCase(ASAP_1_0.ANY_FORMAT.toString()));
        Assert.assertTrue(interestPDU.getSender().equalsIgnoreCase(sender));
        Assert.assertTrue(interestPDU.senderSet());
        Assert.assertFalse(interestPDU.recipientSet());
        Assert.assertFalse(interestPDU.eraFromSet());
        Assert.assertFalse(interestPDU.eraToSet());
    }

    @Test
    public void sendAndReceiveInterestEncrypted() throws IOException, ASAPException {
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_ID);

        // add Bob
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID);
        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_ID, bobKeyPair);

        ASAP_1_0 asapModemAlice = new ASAP_Modem_Impl(keyStorageAlice);
        ASAP_1_0 asapModemBob = new ASAP_Modem_Impl(keyStorageBob);

        String sender = ALICE_ID;
        String recipient = BOB_ID;
        String channel = "AliceURI";
        String format = "format";

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        /////////////////////// encrypted
        asapModemAlice.interest(sender, recipient, format, channel, os,false, true);

        // try t read output
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        ASAP_PDU_1_0 asap_pdu_1_0 = asapModemBob.readPDU(is);

        ASAP_Interest_PDU_1_0 interestPDU = (ASAP_Interest_PDU_1_0) asap_pdu_1_0;

        Assert.assertTrue(interestPDU.getChannelUri().equalsIgnoreCase(channel));
        Assert.assertTrue(interestPDU.getFormat().equalsIgnoreCase(format));
        Assert.assertTrue(interestPDU.getSender().equalsIgnoreCase(sender));
        Assert.assertTrue(interestPDU.getRecipient().equalsIgnoreCase(recipient));
    }

    @Test
    public void sendAndReceiveInterestSigned() throws IOException, ASAPException {
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_ID);

        // add Bob
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID);
        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_ID, bobKeyPair);
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair());

        ASAP_1_0 asapModemAlice = new ASAP_Modem_Impl(keyStorageAlice);
        ASAP_1_0 asapModemBob = new ASAP_Modem_Impl(keyStorageBob);

        String sender = ALICE_ID;
        String recipient = BOB_ID;
        String channel = "AliceURI";
        String format = "format";

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        /////////////////////// encrypted
        asapModemAlice.interest(sender, recipient, format, channel, os,true, false);

        // try t read output
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        ASAP_PDU_1_0 asap_pdu_1_0 = asapModemBob.readPDU(is);

        ASAP_Interest_PDU_1_0 interestPDU = (ASAP_Interest_PDU_1_0) asap_pdu_1_0;

        Assert.assertTrue(interestPDU.getChannelUri().equalsIgnoreCase(channel));
        Assert.assertTrue(interestPDU.getFormat().equalsIgnoreCase(format));
        Assert.assertTrue(interestPDU.getSender().equalsIgnoreCase(sender));
        Assert.assertTrue(interestPDU.getRecipient().equalsIgnoreCase(recipient));
        Assert.assertTrue(interestPDU.verified());
    }

    @Test
    public void sendAndReceiveInterestSignedAndEncrypted() throws IOException, ASAPException {
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_ID);

        // add Bob
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID);
        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_ID,bobKeyPair);
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair());

        ASAP_1_0 asapModemAlice = new ASAP_Modem_Impl(keyStorageAlice);
        ASAP_1_0 asapModemBob = new ASAP_Modem_Impl(keyStorageBob);

        String sender = ALICE_ID;
        String recipient = BOB_ID;
        String channel = "AliceURI";
        String format = "format";

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        /////////////////////// encrypted
        asapModemAlice.interest(sender, recipient, format, channel, os,true, true);

        // try t read output
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        ASAP_PDU_1_0 asap_pdu_1_0 = asapModemBob.readPDU(is);

        ASAP_Interest_PDU_1_0 interestPDU = (ASAP_Interest_PDU_1_0) asap_pdu_1_0;

        Assert.assertTrue(interestPDU.getChannelUri().equalsIgnoreCase(channel));
        Assert.assertTrue(interestPDU.getFormat().equalsIgnoreCase(format));
        Assert.assertTrue(interestPDU.getSender().equalsIgnoreCase(sender));
        Assert.assertTrue(interestPDU.getRecipient().equalsIgnoreCase(recipient));
        Assert.assertTrue(interestPDU.encrypted());
        Assert.assertTrue(interestPDU.verified());
    }

    @Test(expected = ASAPSecurityException.class)
    public void sendEncryptedMessageNotToRecipient() throws IOException, ASAPException {
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_ID);

        // add Bob
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID);
        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_ID, bobKeyPair);

        // add Clara
        BasicCryptoKeyStorage keyStorageClara = new BasicCryptoKeyStorage(CLARA_ID);

        ASAP_1_0 asapModemAlice = new ASAP_Modem_Impl(keyStorageAlice);
        //ASAP_1_0 asapModemBob = new ASAP_Modem_Impl(keyStorageBob);
        ASAP_1_0 asapModemClara = new ASAP_Modem_Impl(keyStorageClara);

        String sender = ALICE_ID;
        String recipient = BOB_ID;
        String channel = "AliceURI";
        String format = "format";

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        /////////////////////// create encrypted message for bob
        asapModemAlice.interest(sender, recipient, format, channel, os,false, true);

        // try to read output
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        // and send to clara. It should fail, but:
        System.out.println("We need a feature that handles / redistributes unencryptable messages");
        asapModemClara.readPDU(is);
//        ASAP_PDU_1_0 asap_pdu_1_0 = asapModemClara.readPDU(is);
    }

    ////////////////////           assimilate          /////////////////////////////////////////
    @Test
    public void sendAndReceiveAssimilate() throws IOException, ASAPException {
        ASAP_1_0 protocolEngine = new ASAP_Modem_Impl();

        String sender = "Alice";
        String recipient = "Bob";
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

        protocolEngine.assimilate(sender, recipient, format, channel, era, offsetsList, data, os,false);

        // try t read output
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        ASAP_PDU_1_0 asap_pdu_1_0 = protocolEngine.readPDU(is);

        ASAP_AssimilationPDU_1_0 assimilationPDU = (ASAP_AssimilationPDU_1_0) asap_pdu_1_0;

        Assert.assertTrue(assimilationPDU.getChannelUri().equalsIgnoreCase(channel));
        Assert.assertTrue(assimilationPDU.getFormat().equalsIgnoreCase(format));
        Assert.assertTrue(assimilationPDU.getSender().equalsIgnoreCase(sender));
        Assert.assertTrue(assimilationPDU.getRecipientPeer().equalsIgnoreCase(recipient));
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

    @Test
    public void sendAndReceiveAssimilateSignedAndEncrypted() throws IOException, ASAPException {
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_ID);

        // add Bob
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID);
        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_ID,bobKeyPair);
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair());

        ASAP_1_0 asapModemAlice = new ASAP_Modem_Impl(keyStorageAlice);
        ASAP_1_0 asapModemBob = new ASAP_Modem_Impl(keyStorageBob);

        String sender = ALICE_ID;
        String recipient = BOB_ID;
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


        asapModemAlice.assimilate(sender, recipient, format, channel, era, offsetsList, data, os,
                true, true);

        // try t read output
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        ASAP_PDU_1_0 asap_pdu_1_0 = asapModemBob.readPDU(is);

        ASAP_AssimilationPDU_1_0 assimilationPDU = (ASAP_AssimilationPDU_1_0) asap_pdu_1_0;

        Assert.assertTrue(assimilationPDU.getChannelUri().equalsIgnoreCase(channel));
        Assert.assertTrue(assimilationPDU.getFormat().equalsIgnoreCase(format));
        Assert.assertTrue(assimilationPDU.getSender().equalsIgnoreCase(sender));
        Assert.assertTrue(assimilationPDU.getRecipientPeer().equalsIgnoreCase(recipient));
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

    @Test
    public void sendAndReceiveAssimilateSigned() throws IOException, ASAPException {
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_ID);

        // add Bob
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID);
        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_ID,bobKeyPair);
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair());

        ASAP_1_0 asapModemAlice = new ASAP_Modem_Impl(keyStorageAlice);
        ASAP_1_0 asapModemBob = new ASAP_Modem_Impl(keyStorageBob);

        String sender = ALICE_ID;
        String recipient = BOB_ID;
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


        asapModemAlice.assimilate(sender, recipient, format, channel, era, offsetsList, data, os,
                true, false);

        // try t read output
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        ASAP_PDU_1_0 asap_pdu_1_0 = asapModemBob.readPDU(is);

        ASAP_AssimilationPDU_1_0 assimilationPDU = (ASAP_AssimilationPDU_1_0) asap_pdu_1_0;

        Assert.assertTrue(assimilationPDU.getChannelUri().equalsIgnoreCase(channel));
        Assert.assertTrue(assimilationPDU.getFormat().equalsIgnoreCase(format));
        Assert.assertTrue(assimilationPDU.getSender().equalsIgnoreCase(sender));
        Assert.assertTrue(assimilationPDU.getRecipientPeer().equalsIgnoreCase(recipient));
        Assert.assertEquals(assimilationPDU.getEra(), era);

        byte[] data_received = assimilationPDU.getData();
        List<Integer> offsets_received = assimilationPDU.getMessageOffsets();
        // one entry assumed
        int offsetInt = offsets_received.get(0);

        byte[] data_r1 = new byte[offsetInt];
        for(int i = 0; i < offsetInt; i++) {
            data_r1[i] = data_received[i];
        }

        int remainingByteNumber = data_received.length-offsetInt;
        byte[] data_r2 = new byte[remainingByteNumber];
        for(int i = 0; i < remainingByteNumber; i++) {
            data_r2[i] = data_received[i+offsetInt];
        }

        Assert.assertTrue(new String(data_r1).equalsIgnoreCase(testString1));
        Assert.assertTrue(new String(data_r2).equalsIgnoreCase(testString2));

        //// taken from ASAPEngine.handleAssimilate
        List<Integer> messageOffsets = assimilationPDU.getMessageOffsets();

        // iterate messages and stream into chunk
        InputStream protocolInputStream = assimilationPDU.getInputStream();
        long offset = 0;
        for(long nextOffset : messageOffsets) {
            //<<<<<<<<<<<<<<<<<<debug
            StringBuilder b = new StringBuilder();
            b.append("going to read message: [");
            b.append(offset);
            b.append(", ");
            b.append(nextOffset);
            b.append(")");
            System.out.println(b.toString());
            //>>>>>>>>>>>>>>>>>>>debug
            // simulate read
            long length = nextOffset - offsetInt;
            while(length-- > 0) {
                protocolInputStream.read();
            }
            offset = nextOffset;
        }
        StringBuilder b = new StringBuilder();
        b.append("going to read last message: from offset ");
        b.append(offset);
        b.append(" to end of file - total length: ");
        b.append(assimilationPDU.getLength());
        System.out.println(b.toString());
    }

    @Test
    public void sendAndReceiveAssimilateEncrypted() throws IOException, ASAPException {
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_ID);

        // add Bob
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID);
        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_ID,bobKeyPair);
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair());

        ASAP_1_0 asapModemAlice = new ASAP_Modem_Impl(keyStorageAlice);
        ASAP_1_0 asapModemBob = new ASAP_Modem_Impl(keyStorageBob);

        String sender = ALICE_ID;
        String recipient = BOB_ID;
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


        asapModemAlice.assimilate(sender, recipient, format, channel, era, offsetsList, data, os,
                false, true);

        // try t read output
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        ASAP_PDU_1_0 asap_pdu_1_0 = asapModemBob.readPDU(is);

        ASAP_AssimilationPDU_1_0 assimilationPDU = (ASAP_AssimilationPDU_1_0) asap_pdu_1_0;

        Assert.assertTrue(assimilationPDU.getChannelUri().equalsIgnoreCase(channel));
        Assert.assertTrue(assimilationPDU.getFormat().equalsIgnoreCase(format));
        Assert.assertTrue(assimilationPDU.getSender().equalsIgnoreCase(sender));
        Assert.assertTrue(assimilationPDU.getRecipientPeer().equalsIgnoreCase(recipient));
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
}
