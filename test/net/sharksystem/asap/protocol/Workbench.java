package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.crypto.TestASAPKeyStorage;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;

public class Workbench {
    public static final String ALICE_ID = "Alice";
    public static final String BOB_ID = "Bob";


    @Test
    public void sendAndReceiveInterestSignedAndEncrypted() throws IOException, ASAPException {
        TestASAPKeyStorage keyStorageAlice = new TestASAPKeyStorage(ALICE_ID);

        // add Bob
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID);
        TestASAPKeyStorage keyStorageBob = new TestASAPKeyStorage(BOB_ID,bobKeyPair);
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
}
