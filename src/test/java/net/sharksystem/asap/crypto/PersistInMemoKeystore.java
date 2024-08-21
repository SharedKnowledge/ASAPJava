package net.sharksystem.asap.crypto;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import org.junit.Test;

import java.io.IOException;

import static net.sharksystem.utils.testsupport.TestConstants.ALICE_ID;

public class PersistInMemoKeystore {
    /**
     * It is not really a good idea to implement a serialization like this. It is extremely
     * vulnerable... But is makes testing a bit easier.
     */
    @Test
    public void writeAndRestore() throws ASAPException, IOException {
        InMemoASAPKeyStore imKeystore = new InMemoASAPKeyStore("TestPeer");
        imKeystore.generateKeyPair();

        byte[] memento = imKeystore.createMemento();
        imKeystore.restoreFromMemento(memento);
    }
}
