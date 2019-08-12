package net.sharksystem.asap;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author thsc
 */
public class WhiteBoxTests {
    
    public WhiteBoxTests() {
    }
    
    //@Test
    public void chunksIter() throws IOException, ASAPException {
        ASAPEngine bobEngine = ASAPEngineFS.getASAPEngine("bob");
        
        List<ASAPChunk> chunks = bobEngine.getStorage().getChunks(0);
        Iterator<CharSequence> messages = chunks.iterator().next().getMessages();
        while(messages.hasNext()) {
            CharSequence message = messages.next();
            System.out.println(message);
        }
    }

    @Test
    public void writeReadByteMessages() throws IOException, ASAPException {
        String folder = "writeReadByteMessagesTest";
        String uri = "test://anURI";
        String firstMessage = "first message";
        String secondMessage = "second message";

        ASAPEngineFS.removeFolder(folder);
        ASAPStorage storage = ASAPEngineFS.getASAPStorage(folder);

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
}
