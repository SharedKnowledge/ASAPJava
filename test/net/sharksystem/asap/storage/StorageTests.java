package net.sharksystem.asap.storage;

import net.sharksystem.TestConstants;
import net.sharksystem.asap.*;
import net.sharksystem.asap.engine.ASAPEngine;
import net.sharksystem.asap.engine.ASAPEngineFS;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

public class StorageTests {
    static final String ROOTFOLDER = TestConstants.ROOT_DIRECTORY + StorageTests.class.getSimpleName() + "/";
    static final String ALICEFOLDER = ROOTFOLDER  + TestConstants.ALICE_NAME;
    static final String ALICEFOLDER_0 = ALICEFOLDER + "_Test_0";
    static final String ALICEFOLDER_1 = ALICEFOLDER + "_Test_1";
    static final String ALICEFOLDER_2 = ALICEFOLDER + "_Test_2";
    static final String ALICEFOLDER_3 = ALICEFOLDER + "_Test_3";
    static final String ALICEFOLDER_4 = ALICEFOLDER + "_Test_4";

    static final String FORMAT = "TestFormat";
    static final String URI = "test/anURI";
    static final String MESSAGE = "testmessage";

    @Test
    public void removeChannel() throws IOException, ASAPException {
        String testFolder = ALICEFOLDER_0;

        ASAPEngineFS.removeFolder(testFolder);
        ASAPEngine storage =
                ASAPEngineFS.getASAPStorage(
                        TestConstants.ALICE_NAME, testFolder, FORMAT);

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
        String testFolder = ALICEFOLDER_1;

        byte[] bobMessageContent1 = new byte[] {0};
        byte[] aliceMessageContent1 = new byte[] {1};
        byte[] claraMessageContent1 = new byte[] {2};
        byte[] bobMessageContent2 = new byte[] {3};
        byte[] aliceMessageContent2 = new byte[] {4};
        byte[] claraMessageContent2 = new byte[] {5};

        ASAPEngineFS.removeFolder(testFolder);

        ASAPEngineFS storage = (ASAPEngineFS) ASAPEngineFS.getASAPStorage(TestConstants.ALICE_NAME, testFolder, FORMAT);
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

        messages.getMessage(5, true);


        byte expected = 0;
        while(messageIter.hasNext()) {
            System.out.print(", " + expected);
            if(expected == 4) {
                int i = 42; // debug breakpoint
            }
            byte[] msg = messageIter.next();
            Assert.assertEquals(expected, msg[0]);
            expected++;
        }
    }

    @Test
    public void channelTest2() throws IOException, ASAPException {
        String testFolder = ALICEFOLDER_2;

        byte[] aliceMessageContent1 = new byte[] {0};

        ASAPEngineFS.removeFolder(testFolder);

        ASAPEngineFS storage = (ASAPEngineFS) ASAPEngineFS.getASAPStorage(TestConstants.ALICE_NAME, testFolder, FORMAT);
        storage.add(URI, aliceMessageContent1);
        ASAPMessages messages = storage.getChannel(URI).getMessages(false);

        Iterator<byte[]> messageIter = messages.getMessages();
        byte expected = 0;
        while(messageIter.hasNext()) {
            byte[] msg = messageIter.next();
            Assert.assertEquals(expected, msg[0]);
            expected++;
        }
    }

    @Test
    public void channelTest3() throws IOException, ASAPException {
        String testFolder = ALICEFOLDER_3;

        byte[] bobMessageContent1 = new byte[] {0};
        byte[] bobMessageContent2 = new byte[] {1};
        byte[] claraMessageContent1 = new byte[] {2};
        byte[] claraMessageContent2 = new byte[] {3};
        byte[] aliceMessageContent1 = new byte[] {4};
        byte[] aliceMessageContent2 = new byte[] {5};

        ASAPEngineFS.removeFolder(testFolder);

        ASAPEngineFS storage = (ASAPEngineFS) ASAPEngineFS.getASAPStorage(TestConstants.ALICE_NAME, testFolder, FORMAT);
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

        messages.getMessage(5, true);

        byte expected = 0;
        while(messageIter.hasNext()) {
            System.out.print(", " + expected);
            if(expected == 4) {
                int i = 42; // debug breakpoint
            }
            byte[] msg = messageIter.next();
            Assert.assertEquals(expected, msg[0]);
            expected++;
        }
    }

    @Test
    public void channelTest4() throws IOException, ASAPException {
        String testFolder = ALICEFOLDER_4;

        byte[] aliceMessageContent1 = new byte[] {0};
        byte[] aliceMessageContent2 = new byte[] {1};
        byte[] bobMessageContent1 = new byte[] {2};
        byte[] bobMessageContent2 = new byte[] {3};
        byte[] claraMessageContent1 = new byte[] {4};
        byte[] claraMessageContent2 = new byte[] {5};

        ASAPEngineFS.removeFolder(testFolder);

        ASAPEngineFS storage = (ASAPEngineFS) ASAPEngineFS.getASAPStorage(TestConstants.ALICE_NAME, testFolder, FORMAT);
        storage.add(URI, aliceMessageContent1);
        storage.add(URI, aliceMessageContent2);

        ASAPStorage bobStorage = storage.getIncomingStorage(TestConstants.BOB_NAME);
        bobStorage.add(URI, bobMessageContent1);
        bobStorage.add(URI, bobMessageContent2);

        ASAPStorage claraStorage = storage.getIncomingStorage(TestConstants.CLARA_NAME);
        claraStorage.add(URI, claraMessageContent1);
        claraStorage.add(URI, claraMessageContent2);

        ASAPMessages messages = storage.getChannel(URI).getMessages(false);

        Assert.assertEquals(0, messages.getMessage(0, true)[0]);
        Assert.assertEquals(1, messages.getMessage(1, true)[0]);

        byte[] msg = messages.getMessage(2, true);
        Assert.assertTrue(msg[0] == 2 || msg[0] == 4); // comes from B or C
        msg = messages.getMessage(3, true);
        Assert.assertTrue(msg[0] == 3 || msg[0] == 5); // comes from B or C
        msg = messages.getMessage(4, true);
        Assert.assertTrue(msg[0] == 2 || msg[0] == 4); // comes from B or C
        msg = messages.getMessage(5, true);
        Assert.assertTrue(msg[0] == 3 || msg[0] == 5); // comes from B or C
    }
}
