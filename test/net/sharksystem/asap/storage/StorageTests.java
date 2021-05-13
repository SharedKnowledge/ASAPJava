package net.sharksystem.asap.storage;

import net.sharksystem.TestConstants;
import net.sharksystem.asap.*;
import net.sharksystem.asap.engine.ASAPEngine;
import net.sharksystem.asap.engine.ASAPEngineFS;
import net.sharksystem.utils.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

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

    @Test
    public void channelTest() throws IOException, ASAPException {
        byte[] bobMessageContent1 = new byte[] {0};
        byte[] aliceMessageContent1 = new byte[] {1};
        byte[] claraMessageContent1 = new byte[] {2};
        byte[] bobMessageContent2 = new byte[] {3};
        byte[] aliceMessageContent2 = new byte[] {4};
        byte[] claraMessageContent2 = new byte[] {5};

        ASAPEngineFS.removeFolder(ALICEFOLDER);

        ASAPEngineFS storage = (ASAPEngineFS) ASAPEngineFS.getASAPStorage(TestConstants.ALICE_NAME, ALICEFOLDER, FORMAT);
        storage.add(URI, aliceMessageContent1);
        storage.add(URI, aliceMessageContent2);

        ASAPStorage bobStorage = storage.getIncomingStorage(TestConstants.BOB_NAME);
        bobStorage.add(URI, bobMessageContent1);
        bobStorage.add(URI, bobMessageContent2);

        ASAPStorage claraStorage = storage.getIncomingStorage(TestConstants.CLARA_NAME);
        claraStorage.add(URI, claraMessageContent1);
        claraStorage.add(URI, claraMessageContent2);

        ASAPMessages messages = storage.getChannel(URI).getMessages(new ASAPMessageCompare() {
            @Override
            public boolean earlier(byte[] messageA, byte[] messageB) {
                return messageA[0] < messageB[0];
            }
        });

        Iterator<byte[]> messageIter = messages.getMessages();
        byte expected = 0;
        while(messageIter.hasNext()) {
            byte[] msg = messageIter.next();
            Assert.assertEquals(expected, msg[0]);
            expected++;
        }
    }
}
