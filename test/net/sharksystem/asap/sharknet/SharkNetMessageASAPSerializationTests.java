package net.sharksystem.asap.sharknet;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.crypto.BasicCryptoKeyStorage;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.security.KeyPair;

public class SharkNetMessageASAPSerializationTests {
    public static final String WORKING_SUB_DIRECTORY = "sharkNetTests/";
    public static final String ALICE_ID = "Alice";
    public static final String BOB_ID = "Bob";
    public static final String MESSAGE = "Hi";
    public static final String URI = "sn2://all";

    @Test
    public void serializationTestPlain() throws ASAPException, IOException {
        byte[] serializedSNMessage = SharkNetMessage.serializeMessage(
                MESSAGE.getBytes(), URI, BOB_ID, false, false, ALICE_ID, null);

        SharkNetMessage sharkNetMessage =
                SharkNetMessage.parseMessage(serializedSNMessage, ALICE_ID, URI, BOB_ID, null);

        Assert.assertEquals(MESSAGE, new String(sharkNetMessage.getContent()));
        Assert.assertEquals(ALICE_ID, sharkNetMessage.getSender());
        Assert.assertFalse(sharkNetMessage.verified());
        Assert.assertFalse(sharkNetMessage.encrypted());
    }

    @Test
    public void serializationTestSigned() throws ASAPException, IOException {
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_ID);
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID); // Alice knows Bob

        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_ID, bobKeyPair);
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair()); // Bob knows Alice

        byte[] serializedSNMessage = SharkNetMessage.serializeMessage(
                MESSAGE.getBytes(), URI, BOB_ID, true, false, ALICE_ID, keyStorageAlice);

        SharkNetMessage sharkNetMessage =
                SharkNetMessage.parseMessage(serializedSNMessage, ALICE_ID, URI, BOB_ID, keyStorageBob);

        Assert.assertEquals(MESSAGE, new String(sharkNetMessage.getContent()));
        Assert.assertEquals(ALICE_ID, sharkNetMessage.getSender());
        Assert.assertTrue(sharkNetMessage.verified());
        Assert.assertFalse(sharkNetMessage.encrypted());
    }

    @Test
    public void serializationTestSignedNotVerified() throws ASAPException, IOException {
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_ID);
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID);
        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_ID, bobKeyPair);
        // Bob does not know Alice

        byte[] serializedSNMessage = SharkNetMessage.serializeMessage(
                MESSAGE.getBytes(), URI, BOB_ID, true, false, ALICE_ID, keyStorageAlice);

        SharkNetMessage sharkNetMessage =
                SharkNetMessage.parseMessage(serializedSNMessage, ALICE_ID, URI, BOB_ID, keyStorageBob);

        Assert.assertEquals(MESSAGE, new String(sharkNetMessage.getContent()));
        Assert.assertEquals(ALICE_ID, sharkNetMessage.getSender());
        Assert.assertFalse(sharkNetMessage.verified());
        Assert.assertFalse(sharkNetMessage.encrypted());
    }

    @Test
    public void serializationTestEncrypted() throws ASAPException, IOException {
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_ID);
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID); // Alice knows Bob

        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_ID, bobKeyPair);
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair()); // Bob knows Alice

        byte[] serializedSNMessage = SharkNetMessage.serializeMessage(
                MESSAGE.getBytes(), URI, BOB_ID, false, true, ALICE_ID, keyStorageAlice);

        SharkNetMessage sharkNetMessage =
                SharkNetMessage.parseMessage(serializedSNMessage, ALICE_ID, URI, BOB_ID, keyStorageBob);

        Assert.assertEquals(MESSAGE, new String(sharkNetMessage.getContent()));
        Assert.assertEquals(ALICE_ID, sharkNetMessage.getSender());
        Assert.assertFalse(sharkNetMessage.verified());
        Assert.assertTrue(sharkNetMessage.encrypted());
    }

    @Test
    public void serializationTestEncryptedAndSigned() throws ASAPException, IOException {
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_ID);
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID); // Alice knows Bob

        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_ID, bobKeyPair);
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair()); // Bob knows Alice

        byte[] serializedSNMessage = SharkNetMessage.serializeMessage(
                MESSAGE.getBytes(), URI, BOB_ID, true, true, ALICE_ID, keyStorageAlice);

        SharkNetMessage sharkNetMessage =
                SharkNetMessage.parseMessage(serializedSNMessage, ALICE_ID, URI, BOB_ID, keyStorageBob);

        Assert.assertEquals(MESSAGE, new String(sharkNetMessage.getContent()));
        Assert.assertEquals(ALICE_ID, sharkNetMessage.getSender());
        Assert.assertTrue(sharkNetMessage.verified());
        Assert.assertTrue(sharkNetMessage.encrypted());
    }


}
