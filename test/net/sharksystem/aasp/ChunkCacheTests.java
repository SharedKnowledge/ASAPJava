package net.sharksystem.aasp;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

public class ChunkCacheTests {
    public static final String TEST_URI = "TEST_URI";
    private static final String MESSAGE_ONE = "message one";
    private static final String MESSAGE_TWO = "message two";


    @Test
    public void chunkTest1() throws IOException, AASPException {

        // open a chunk storage
        AASPStorage storage = AASPEngineFS.getAASPChunkStorage("AASPChunkCacheTest");

        int era = storage.getEra();

        AASPChunkStorage chunkStorage = storage.getChunkStorage();

        AASPChunk chunk = chunkStorage.getChunk(TEST_URI, era);

        chunk.add(MESSAGE_ONE);

        storage.newEra();

        int newEra = storage.getEra();

        Assert.assertEquals(AASPEngine.nextEra(era), newEra);

        chunk = chunkStorage.getChunk(TEST_URI, newEra);

        chunk.add(MESSAGE_TWO);

        AASPChunkCache aaspChunkCache = chunkStorage.getAASPChunkCache(TEST_URI, era, newEra);

        Iterator<CharSequence> messages = aaspChunkCache.getMessages(true);

        Assert.assertTrue(messages.hasNext());
        CharSequence message = messages.next();
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_ONE));

        message = messages.next();
        Assert.assertTrue(message.toString().equalsIgnoreCase(MESSAGE_TWO));

    }
}
