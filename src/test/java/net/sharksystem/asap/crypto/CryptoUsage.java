package net.sharksystem.asap.crypto;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static net.sharksystem.asap.apps.testsupport.TestConstants.ALICE_ID;
import static net.sharksystem.asap.apps.testsupport.TestConstants.BOB_ID;

public class CryptoUsage {
    private InMemoASAPKeyStore aliceKeyStorage;
    private InMemoASAPKeyStore bobKeyStorage;

    @Before
    public void setupASAPKeyStores() throws ASAPSecurityException {
        this.aliceKeyStorage = new InMemoASAPKeyStore(ALICE_ID);
        this.bobKeyStorage = new InMemoASAPKeyStore(BOB_ID);

        // simulate key exchange
        this.aliceKeyStorage.addKeyPair(BOB_ID, bobKeyStorage.getKeyPair());
        this.bobKeyStorage.addKeyPair(ALICE_ID, aliceKeyStorage.getKeyPair());
    }

    @Test
    public void signVerifyUsage() throws ASAPSecurityException {
        String messageString = "From Alice signed";
        // produce bytes
        byte[] messageBytes = messageString.getBytes();
        // alice signs a message
        byte[] signedMessage = ASAPCryptoAlgorithms.sign(messageBytes, aliceKeyStorage);

        // verify
        Assert.assertTrue(ASAPCryptoAlgorithms.verify(messageBytes, signedMessage, ALICE_ID, bobKeyStorage));
    }

    @Test
    public void encryptedPackageUsageExample() throws IOException, ASAPException {
        String messageString = "From Alice, encrypted for Bob";
        // produce bytes
        byte[] messageBytes = messageString.getBytes();

        // produce encryption package: encrypt with new session key, encrypt session key with receivers public key
        byte[] encryptedMessagePackageBytes = ASAPCryptoAlgorithms.produceEncryptedMessagePackage(
                messageBytes, // message that is encrypted
                BOB_ID, // recipient id
                aliceKeyStorage // key store sender
        );

        // package is sent e.g. with ASAP
        byte[] receivedEncryptedPackageBytes = encryptedMessagePackageBytes;

        // receiver creates package from byte[] - will fail if we are not recipient
        ASAPCryptoAlgorithms.EncryptedMessagePackage receivedEncryptedPackage =
                ASAPCryptoAlgorithms.parseEncryptedMessagePackage(receivedEncryptedPackageBytes);

        // decrypt message
        byte[] receivedMessageBytes =
                ASAPCryptoAlgorithms.decryptPackage(receivedEncryptedPackage, bobKeyStorage);

        // must be the same
        Assert.assertArrayEquals(messageBytes, receivedMessageBytes);
    }
}
