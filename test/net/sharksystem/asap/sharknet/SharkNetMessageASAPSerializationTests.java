package net.sharksystem.asap.sharknet;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.crypto.BasicCryptoKeyStorage;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.security.KeyPair;
import java.util.HashSet;
import java.util.Set;

public class SharkNetMessageASAPSerializationTests {
    public static final String WORKING_SUB_DIRECTORY = "sharkNetTests/";
    public static final String ALICE_ID = "Alice";
    public static final String BOB_ID = "Bob";
    public static final String CLARA_ID = "Clara";
    public static final String MESSAGE = "Hi";
    public static final String URI = "sn2://all";
    public static final String ALICE_FOLDER = WORKING_SUB_DIRECTORY + "/" + ALICE_ID;

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

    @Test
    public void snTestSignedMultipleRecipients() throws ASAPException, IOException, InterruptedException {
        // Alice
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_ID);

        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_ID);
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair()); // Bob knows Alice

        SharkNetPeer snPeerAlice = new SharkNetPeerFS(ALICE_ID, ALICE_FOLDER, keyStorageAlice);
        SharkNetMessageReceivedListener snListenerAlice = new SharkNetMessageReceivedListener();
        snPeerAlice.addSharkNetMessageListener(snListenerAlice);

        Set<CharSequence> recipients = new HashSet<>();
        recipients.add(BOB_ID);
        recipients.add(CLARA_ID);
        // create Message
        SharkNetMessage alice2bobAndClara = new SharkNetMessage(
                MESSAGE.getBytes(), URI, ALICE_ID, recipients, true, keyStorageAlice);

        byte[] asapMessage = alice2bobAndClara.getSerializedMessage();

        // parse
        SharkNetMessage receivedMessage =
                SharkNetMessage.parseMessage(asapMessage, ALICE_ID, URI, ALICE_ID, keyStorageBob);

        Assert.assertEquals(MESSAGE, new String(receivedMessage.getContent()));
        Assert.assertEquals(2, receivedMessage.getRecipients().size());
        Assert.assertEquals(ALICE_ID, receivedMessage.getSender());
        Assert.assertTrue(receivedMessage.verified());
        Assert.assertFalse(receivedMessage.encrypted());
    }
}