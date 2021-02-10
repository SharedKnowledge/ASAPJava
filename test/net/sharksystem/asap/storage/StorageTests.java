package net.sharksystem.asap.storage;

import net.sharksystem.TestConstants;
import net.sharksystem.asap.ASAPChannel;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.engine.ASAPEngine;
import net.sharksystem.asap.engine.ASAPEngineFS;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class StorageTests {
    static final String ROOTFOLDER = TestConstants.ROOT_DIRECTORY + StorageTests.class.getSimpleName() + "/";
    static final String ALICEFOLDER = ROOTFOLDER  + TestConstants.ALICE_NAME;
    static final String FORMAT = "TestFormat";
    static final String URI = "test/anURI";
    static final String MESSAGE = "testmessage";

    @Test
    public void removeChannel() throws IOException, ASAPException {
        ASAPEngine storage =
                ASAPEngineFS.getASAPStorage(
                        TestConstants.ALICE_NAME, ALICEFOLDER, FORMAT);

        // fill it
        storage.add(URI, MESSAGE);
        storage.newEra();
        storage.add(URI, MESSAGE);

        ASAPChannel channel = storage.getChannel(URI);
        Assert.assertEquals(2, channel.getMessages().size());

        // remove it
        storage.removeChannel(URI);

        // must be empty now
        //channel = storage.getChannel(URI);
        Assert.assertEquals(0, channel.getMessages().size());
    }
}
