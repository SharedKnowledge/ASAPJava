package net.sharksystem.asap;

import net.sharksystem.asap.util.ASAPPeerHandleConnectionThread;
import net.sharksystem.asap.util.Helper;
import net.sharksystem.cmdline.ExampleASAPChunkReceivedListener;
import net.sharksystem.cmdline.TCPStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class UsageExamples {
    public static final String WORKING_SUB_DIRECTORY = "asapUsageExamples/";
    public static final String ALICE_PEER_NAME = "Alice";
    public static final String BOB_PEER_NAME = "Bob";
    public static final String CHAT_APP = "chat";
    public static final String CHAT_TOPIC = "topicA";
    public static final int EXAMPLE_PORT = 7070;
    public static final String EXAMPLE_MESSAGE_STRING = "Hi";

    @Test
    public void basicTwoPartyTest() throws IOException, ASAPException, InterruptedException {
        ASAPEngineFS.removeFolder(WORKING_SUB_DIRECTORY); // clean previous version before

        ///// Prepare Alice
        String aliceFolder = WORKING_SUB_DIRECTORY + ALICE_PEER_NAME;

        // ASAPChunkReceivedListener - an example
        ExampleASAPChunkReceivedListener aliceChunkListener = new ExampleASAPChunkReceivedListener(aliceFolder);

        // setup alice peer
        ASAPPeer alicePeer = ASAPPeerFS.createASAPPeer(ALICE_PEER_NAME, aliceFolder, aliceChunkListener);

        // setup chat on alice peer
        ASAPEngine aliceChatEngine = alicePeer.createEngineByFormat(CHAT_APP);

        // create a message
        String messageAlice = EXAMPLE_MESSAGE_STRING;

        // transform to bytes - there are more elaborate ways to produce a byte array of course
        byte[] messageBytes = messageAlice.getBytes();

        // write a message - we are still offline
        aliceChatEngine.add(CHAT_TOPIC, messageBytes);

        ///// Prepare Bob
        String bobFolder = WORKING_SUB_DIRECTORY + BOB_PEER_NAME;

        // ASAPChunkReceivedListener - an example
        ExampleASAPChunkReceivedListener bobChunkListener = new ExampleASAPChunkReceivedListener(bobFolder);

        // setup alice peer
        ASAPPeer bobPeer = ASAPPeerFS.createASAPPeer(BOB_PEER_NAME, bobFolder, bobChunkListener);

        // setup chat on alice peer
        ASAPEngine bobChatEngine = bobPeer.createEngineByFormat(CHAT_APP);

        /////////////// create a connection - in real apps it is presumably a bluetooth wifi direct etc. connection
        // TCPStream is a helper class for connection establishment
        TCPStream aliceStream = new TCPStream(EXAMPLE_PORT, true, "alice2bob");
        TCPStream bobStream = new TCPStream(EXAMPLE_PORT, false, "b2a");

        // start tcp server or client and try to connect
        aliceStream.start();
        bobStream.start();

        // wait until connection is established
        aliceStream.waitForConnection();
        bobStream.waitForConnection();
        //////////////// end of connection establishment - a simulation in some way - but real enough. It is real tcp.

        // let both asap peers run an asap session
        ASAPPeerHandleConnectionThread aliceThread = new ASAPPeerHandleConnectionThread(alicePeer,
                aliceStream.getInputStream(), aliceStream.getOutputStream());

        // alice is up and running in a thread
        aliceThread.start();

        // run bob in this test thread
        bobPeer.handleConnection(bobStream.getInputStream(), bobStream.getOutputStream());

        // at this point give both asap engines some time to run their asap session - then we check what happened.
        Thread.sleep(1000);

        // we assume the asap session was performed

        // bob chunk received listener must have received something
        List<ExampleASAPChunkReceivedListener.ASAPChunkReceivedParameters> receivedList =
                bobChunkListener.getReceivedList();

        Assert.assertNotNull(receivedList);
        Assert.assertFalse(receivedList.isEmpty());

        // there must be a single entry - get it
        Assert.assertTrue(receivedList.size() == 1);
        ExampleASAPChunkReceivedListener.ASAPChunkReceivedParameters parameters = receivedList.get(0);

        // get chunk storage
        ASAPChunkStorage receivedChunkStorage = bobChatEngine.getReceivedChunksStorage(parameters.getSender());

        // get chunk
        ASAPChunk chunk = receivedChunkStorage.getChunk(parameters.getUri(), parameters.getEra());

        Iterator<byte[]> messages = chunk.getMessages();

        // there must a one and only one
        byte[] messageBytesReceived = messages.next();

        // convert to String
        String receivedMessage = new String(messageBytesReceived);

        System.out.println(bobChatEngine.getOwner() + " received a message: " + receivedMessage);

        Assert.assertTrue(receivedMessage.equals(EXAMPLE_MESSAGE_STRING));

        // make it a bit easier with a helper class
        ASAPMessages receivedMessages =
                Helper.getMessagesByChunkReceivedInfos(parameters.getFormat(), parameters.getSender(),
                        parameters.getUri(),
                        bobFolder, // peers' root directory!
                        parameters.getEra());

        Iterator<byte[]> msgInter = receivedMessages.getMessages();
        while(msgInter.hasNext()) {
            byte[] msgBytes = msgInter.next();
            String msg = new String(msgBytes);
            System.out.println("message received: " + msg);
        }
    }
}
