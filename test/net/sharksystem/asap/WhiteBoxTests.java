package net.sharksystem.asap;

import java.io.IOException;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author thsc
 */
public class WhiteBoxTests {
    public static final String FORMAT = "format";
    public static final String DUMMY_USER = "dummyUser";

    public WhiteBoxTests() {
    }

    @Test
    public void scratch() {
        int maxEra = 2^8-1;
        int i = 42;
    }

    @Test
    public void writeReadByteMessages() throws IOException, ASAPException {
        String folder = "tests/writeReadByteMessagesTest";
        ASAPEngineFS.removeFolder(folder);

        String uri = "test://anURI";
        String firstMessage = "first message";
        String secondMessage = "second message";

        ASAPEngineFS.removeFolder(folder);
        ASAPStorage storage = ASAPEngineFS.getASAPStorage(DUMMY_USER, folder, FORMAT);

        // convert message into bytes
        byte[] messageBytes = firstMessage.getBytes();
        storage.add(uri, messageBytes);

        // convert message into bytes
        messageBytes = secondMessage.getBytes();
        storage.add(uri, messageBytes);

        Iterator<byte[]> byteArrayIter = storage.getChunkStorage().getChunk(uri, storage.getEra()).getMessages();
        messageBytes = byteArrayIter.next();
        String message = new String(messageBytes);
        Assert.assertTrue(message.equals(firstMessage));

        messageBytes = byteArrayIter.next();
        message = new String(messageBytes);
        Assert.assertTrue(message.equals(secondMessage));
    }

    @Test
    public void writeReadStringMessages() throws IOException, ASAPException {
        String folder = "tests/writeReadStringMessagesTest";
        ASAPEngineFS.removeFolder(folder);

        String uri = "test://anURI";
        String firstMessage = "first message";
        String secondMessage = "second message";

        ASAPStorage storage = ASAPEngineFS.getASAPStorage(DUMMY_USER, folder, FORMAT);

        storage.add(uri, firstMessage);
        storage.add(uri, secondMessage);

        Iterator<CharSequence> messageIter = storage.getChunkStorage().getChunk(uri, storage.getEra()).getMessagesAsCharSequence();
        String message = messageIter.next().toString();
        Assert.assertTrue(message.equals(firstMessage));

        message = messageIter.next().toString();
        Assert.assertTrue(message.equals(secondMessage));
    }

    @Test
    public void persistentMessage1() throws IOException, ASAPException {
        String folder = "tests/persistentMessage1";
        ASAPEngineFS.removeFolder(folder);

        String uri = "test://anURI";
        String firstMessage = "first message";

        ASAPStorage storage = ASAPEngineFS.getASAPStorage(DUMMY_USER, folder, FORMAT);
        storage.add(uri, firstMessage);

        // re-create storage
        storage = ASAPEngineFS.getASAPStorage(DUMMY_USER, folder, FORMAT);

        Assert.assertEquals(storage.getChannelURIs().get(0), uri);

        Iterator<CharSequence> messageIter = storage.getChunkStorage().getChunk(uri, storage.getEra()).getMessagesAsCharSequence();
        String message = messageIter.next().toString();
        Assert.assertTrue(message.equals(firstMessage));
    }

    @Test
    public void testExtraData() throws IOException, ASAPException {
        String folder = "tests/testExtraData";
        ASAPEngineFS.removeFolder(folder);

        String uri = "test://anURI";

        ASAPEngineFS.removeFolder(folder);
        ASAPStorage storage = ASAPEngineFS.getASAPStorage(DUMMY_USER, folder, FORMAT);

        String key1 = "key1";
        String value1 = "value1";
        String key2 = "key2";
        String value2 = "value2";

        storage.putExtra(uri, key1, value1);
        storage.putExtra(uri, key2, value2);

        // restore
        storage = ASAPEngineFS.getASAPStorage(DUMMY_USER, folder, FORMAT);
        Assert.assertEquals(storage.getExtra(uri, key1), value1);
        Assert.assertEquals(storage.getExtra(uri, key2), value2);

        Assert.assertEquals(storage.getChannelURIs().get(0), uri);
    }
}
