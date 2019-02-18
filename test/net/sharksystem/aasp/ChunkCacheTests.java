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
        storage.newEra();
        int newEra = storage.getEra();

        Assert.assertEquals(AASPEngine.nextEra(era), newEra);
        chunk = chunkStorage.getChunk(TEST_URI, newEra);
        chunk.add(MESSAGE_TWO);

        Assert.assertEquals(AASPEngine.nextEra(era), newEra);
        chunk = chunkStorage.getChunk(TEST_URI, newEra);
        chunk.add(MESSAGE_THREE);
        chunk = chunkStorage.getChunk(TEST_URI, newEra);
        chunk.add(MESSAGE_FOUR);

        AASPChunkCache aaspChunkCache = chunkStorage.getAASPChunkCache(TEST_URI, era, newEra);

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

        Assert.assertFalse(messages.hasNext());


    }
}
