package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EncryptionTests {
    public static final String ALICE_ID = "Alice";
    public static final String BOB_ID = "Bob";

    @Test
    public void sendAndReceiveInterestCanBeEncrypted() throws IOException, ASAPException {
        TestASAPKeyStorage keyStorage = new TestASAPKeyStorage();
        // add Bob
        keyStorage.createTestPeer(BOB_ID);

        ASAP_1_0 asapModem = new ASAP_Modem_Impl(keyStorage);

        String sender = ALICE_ID;
        String recipient = BOB_ID;
        String channel = "AliceURI";
        String format = "format";

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        /////////////////////// encrypted
        asapModem.interest(sender, recipient, format, channel, os,false, true);

        // try t read output
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        ASAP_PDU_1_0 asap_pdu_1_0 = asapModem.readPDU(is);

        ASAP_Interest_PDU_1_0 interestPDU = (ASAP_Interest_PDU_1_0) asap_pdu_1_0;

        Assert.assertTrue(interestPDU.getChannelUri().equalsIgnoreCase(channel));
        Assert.assertTrue(interestPDU.getFormat().equalsIgnoreCase(format));
        Assert.assertTrue(interestPDU.getSender().equalsIgnoreCase(sender));
        Assert.assertTrue(interestPDU.getRecipient().equalsIgnoreCase(recipient));
    }
}
