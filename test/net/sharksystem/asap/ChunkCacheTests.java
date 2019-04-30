package net.sharksystem.asap;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

public class ChunkCacheTests {
    public static final String TEST_URI = "TEST_URI";
    private static final String MESSAGE_ONE = "message one";
    private static final String MESSAGE_TWO = "message two";
    private static final String MESSAGE_THREE = "message three";
    private static final String MESSAGE_FOUR = "message four";
    private static final String MESSAGE_FIVE = "message five";

    @Test
    public void chunkTest1() throws IOException, ASAPException {
        String rootDirectory = "AASPChunkCacheTest";

        // remove cache first
        ASAPEngineFS.removeFolder(rootDirectory);

        // open a fresh chunk storage
        ASAPStorage storage = ASAPEngineFS.getASAPStorage(rootDirectory);

        int era = storage.getEra();

        ASAPChunkStorage chunkStorage = storage.getChunkStorage();

        ASAPChunk chunk = chunkStorage.getChunk(TEST_URI, era);

        chunk.add(MESSAGE_ONE);
        storage.newEra(); // finish chunk one
        int newEra = storage.getEra();

        // start chunk two
        Assert.assertEquals(ASAPEngine.nextEra(era), newEra);
        chunk = chunkStorage.getChunk(TEST_URI, newEra);
        chunk.add(MESSAGE_TWO);

        chunk.add(MESSAGE_THREE);

        chunk.add(MESSAGE_FOUR);

        ASAPChunkCache ASAPChunkCache = chunkStorage.getASAPChunkCache(TEST_URI, storage.getEra());

        // add message after getting cache

        // position test - chronological order
        CharSequence message = ASAPChunkCache.getMessage(0, true);
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_ONE));

        message = ASAPChunkCache.getMessage(1, true);
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_TWO));

        // position test - newest first
        message = ASAPChunkCache.getMessage(0, false);
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_FOUR));

        message = ASAPChunkCache.getMessage(1, false);
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_THREE));

        chunk.add(MESSAGE_FIVE);

        ASAPChunkCache.sync();
        message = ASAPChunkCache.getMessage(4, true);
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_FIVE));

        Iterator<CharSequence> messages = ASAPChunkCache.getMessages();

        // Iterator test
        Assert.assertTrue(messages.hasNext());
        message = messages.next();
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_ONE));
        message = messages.next();
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_TWO));
        message = messages.next();
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_THREE));
        message = messages.next();
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_FOUR));
        message = messages.next();
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_FIVE));

        Assert.assertFalse(messages.hasNext());


    }
}
