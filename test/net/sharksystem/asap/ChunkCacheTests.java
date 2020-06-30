package net.sharksystem.asap;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ChunkCacheTests {
    public static final String ALICE_FOLDER = "tests/alice";
    public static final String ALICE = "alice";
    public static final String BOB = "bob";
    public static final String CLARA = "clara";
    public static final String DUMMY_USER = "dummyUser";
    public static final String TEST_URI = "TEST_URI";
    public static final String TEST_FORMAT = "format";
    public static final String TEST_APP = "asapApp";
    private static final String MESSAGE_ONE = "message one";
    private static final String MESSAGE_TWO = "message two";
    private static final String MESSAGE_THREE = "message three";
    private static final String MESSAGE_FOUR = "message four";
    private static final String MESSAGE_FIVE = "message five";

    @Test
    public void chunkTest0() throws IOException, ASAPException {
        ASAPEngineFS.removeFolder(ALICE_FOLDER); // clean previous version before
        ASAPEngine aliceStorage = ASAPEngineFS.getASAPStorage(ALICE, ALICE_FOLDER, TEST_APP);

        String[] message = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};

        aliceStorage.add(TEST_URI, message[0]);
        aliceStorage.add(TEST_URI, message[1]);
        aliceStorage.add(TEST_URI, message[2]);
        aliceStorage.add(TEST_URI, message[3]);
        aliceStorage.newEra();
        aliceStorage.add(TEST_URI, message[4]);
        aliceStorage.add(TEST_URI, message[5]);
        aliceStorage.newEra();
        aliceStorage.add(TEST_URI, message[6]);
        aliceStorage.add(TEST_URI, message[7]);
        aliceStorage.newEra();
        aliceStorage.add(TEST_URI, message[8]);
        aliceStorage.add(TEST_URI, message[9]);
        aliceStorage.add(TEST_URI, message[0xA]);
        aliceStorage.newEra();
        aliceStorage.add(TEST_URI, message[0xB]);
        aliceStorage.add(TEST_URI, message[0xC]);
        aliceStorage.add(TEST_URI, message[0xD]);
        aliceStorage.newEra();
        aliceStorage.add(TEST_URI, message[0xE]);
        aliceStorage.add(TEST_URI, message[0xF]);

        // now get message chain
        ASAPMessages chunkChain = aliceStorage.getChunkChain(TEST_URI);
        for(int i = 0; i < 0x10; i++) {
            Assert.assertTrue(chunkChain.getMessage(i, true).toString()
                    .equalsIgnoreCase(message[i]));
        }

        // now get message chain - reverse order
        chunkChain = aliceStorage.getChunkChain(TEST_URI);
        for(int i = 0; i < 0x10; i++) {
            Assert.assertTrue(chunkChain.getMessage(i, false).toString()
                    .equalsIgnoreCase(message[0xF - i]));
        }
    }
}
