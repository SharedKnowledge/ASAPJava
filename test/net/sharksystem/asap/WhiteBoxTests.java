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

    //@Test
    public void writeReadByteMessages() throws IOException, ASAPException {
        String folder = "tests/writeReadByteMessagesTest";
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

        Iterator<byte[]> byteArrayIter = storage.getChunkStorage().getChunk(uri, storage.getEra()).getMessagesAsBytes();
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
        String uri = "test://anURI";
        String firstMessage = "first message";
        String secondMessage = "second message";

        ASAPEngineFS.removeFolder(folder);
        ASAPStorage storage = ASAPEngineFS.getASAPStorage(DUMMY_USER, folder, FORMAT);

        storage.add(uri, firstMessage);
        storage.add(uri, secondMessage);

        Iterator<CharSequence> messageIter = storage.getChunkStorage().getChunk(uri, storage.getEra()).getMessages();
        String message = messageIter.next().toString();
        Assert.assertTrue(message.equals(firstMessage));

        message = messageIter.next().toString();
        Assert.assertTrue(message.equals(secondMessage));
    }
}
