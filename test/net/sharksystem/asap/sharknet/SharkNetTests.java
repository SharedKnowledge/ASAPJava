package net.sharksystem.asap.sharknet;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.crypto.BasicCryptoKeyStorage;
import org.junit.Test;

import java.io.IOException;
import java.security.KeyPair;

public class SharkNetTests implements SharkNetMessageListener {
    public static final String WORKING_SUB_DIRECTORY = "sharkNetTests/";
    public static final String ALICE_ID = "Alice";
    public static final String BOB_ID = "Bob";
    public static final String ALICE_FOLDER = WORKING_SUB_DIRECTORY + "/" + ALICE_ID;
    public static final String MESSAGE = "Hi Bob";
    public static final String URI = "sn2://all";

    @Test
    public void usageTest() throws ASAPException, IOException {
        BasicCryptoKeyStorage keyStorageAlice = new BasicCryptoKeyStorage(ALICE_ID);

        // add Bob
        KeyPair bobKeyPair = keyStorageAlice.createTestPeer(BOB_ID);
        BasicCryptoKeyStorage keyStorageBob = new BasicCryptoKeyStorage(BOB_ID,bobKeyPair);
        keyStorageBob.addKeyPair(ALICE_ID, keyStorageAlice.getKeyPair());

        SharkNetPeer snPeer = new SharkNetPeerFS(ALICE_ID, ALICE_FOLDER, keyStorageAlice);
        snPeer.addSharkNetMessageListener(this);

        snPeer.sendSharkNetMessage(MESSAGE.getBytes(), URI, BOB_ID, true, true);

        // GO AHEAD HERE
    }

    @Override
    public void messageReceived(byte[] message, CharSequence topic, CharSequence senderID, boolean verified, boolean encrypted) {
        System.out.println("something happened - TODO: change this");
    }
}
