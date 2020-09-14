package net.sharksystem.asap;

import net.sharksystem.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.crypto.BasicCryptoKeyStorage;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class BasisCryptoTests {
    public static final String ALICE_ID = "Alice";
    public static final String BOB_ID = "Bob";

    @Test
    public void publicKeyExportImport() throws ASAPSecurityException, IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        BasicCryptoKeyStorage aliceStorage = new BasicCryptoKeyStorage(ALICE_ID);
        // a message
        String msg = "Hi Bob";

        // convert to bytes
        byte[] msgBytes = msg.getBytes();

        // sign a message - with Alice' private key
        byte[] signatureBytes = ASAPCryptoAlgorithms.sign(msgBytes, aliceStorage);

        // Alice could now send message with signature to bob - we are in a test, nothing to do here.

        // Bob need to know Alice' public key to verify - simulate transfer
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        aliceStorage.writePublicKey(baos);

        BasicCryptoKeyStorage bobStorage = new BasicCryptoKeyStorage(BOB_ID);
        // store Alice' public key with Bob
        bobStorage.readPublicKey(ALICE_ID, new ByteArrayInputStream(baos.toByteArray()));

        Assert.assertTrue(ASAPCryptoAlgorithms.verify(msgBytes, signatureBytes, ALICE_ID, bobStorage));
    }
}
