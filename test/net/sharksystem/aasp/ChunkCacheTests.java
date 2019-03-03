package net.sharksystem.aasp;

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
    public void chunkTest1() throws IOException, AASPException {
        String rootDirectory = "AASPChunkCacheTest";

        // remove cache first
        AASPEngineFS.removeFolder(rootDirectory);

        // open a fresh chunk storage
        AASPStorage storage = AASPEngineFS.getAASPChunkStorage(rootDirectory);

        int era = storage.getEra();

        AASPChunkStorage chunkStorage = storage.getChunkStorage();

        AASPChunk chunk = chunkStorage.getChunk(TEST_URI, era);

        chunk.add(MESSAGE_ONE);
        storage.newEra(); // finish chunk one
        int newEra = storage.getEra();

        // start chunk two
        Assert.assertEquals(AASPEngine.nextEra(era), newEra);
        chunk = chunkStorage.getChunk(TEST_URI, newEra);
        chunk.add(MESSAGE_TWO);

        chunk.add(MESSAGE_THREE);

        chunk.add(MESSAGE_FOUR);

        AASPChunkCache aaspChunkCache = chunkStorage.getAASPChunkCache(TEST_URI, storage.getEra());

        // add message after getting cache

        // position test - chronological order
        CharSequence message = aaspChunkCache.getMessage(0, true);
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_ONE));

        message = aaspChunkCache.getMessage(1, true);
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_TWO));

        // position test - newest first
        message = aaspChunkCache.getMessage(0, false);
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_FOUR));

        message = aaspChunkCache.getMessage(1, false);
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_THREE));

        chunk.add(MESSAGE_FIVE);

        aaspChunkCache.sync();
        message = aaspChunkCache.getMessage(4, true);
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_FIVE));

        Iterator<CharSequence> messages = aaspChunkCache.getMessages();

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
