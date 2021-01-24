package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPChunkStorage;
import net.sharksystem.asap.utils.ASAPChunkReceivedTester;
import net.sharksystem.asap.utils.ASAPPeerHandleConnectionThread;
import net.sharksystem.cmdline.TCPStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class Point2PointTests2 {
    public static final String ALICE_BOB_CHAT_URL = "content://aliceAndBob.talk";
    public static final String CHAT_FORMAT = "application/x-sn2-makan";
    public static final String ALICE_ROOT_FOLDER = "tests/Alice";
    public static final String ALICE_APP_FOLDER = ALICE_ROOT_FOLDER + "/appFolder";
    public static final String BOB_ROOT_FOLDER = "tests/Bob";
    public static final String BOB_APP_FOLDER = BOB_ROOT_FOLDER + "/appFolder";
    public static final String ALICE = "Alice";
    public static final String BOB = "Bob";
    public static final String ALICE2BOB_MESSAGE = "Hi Bob";
    public static final String ALICE2BOB_MESSAGE2 = "Hi Bob again";
    public static final String BOB2ALICE_MESSAGE = "Hi Alice";
    public static final String BOB2ALICE_MESSAGE2 = "Hi Alice again";

    private static int portnumber = 7777;

    private int getPortNumber() {
        portnumber++;
        return portnumber;
    }

    @Test
    public void openMessageExchange() throws IOException, ASAPException, InterruptedException {
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare storages                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        ASAPEngineFS.removeFolder(ALICE_ROOT_FOLDER); // clean previous version before
        ASAPEngineFS.removeFolder(BOB_ROOT_FOLDER); // clean previous version before

        // alice writes a message into chunkStorage
        ASAPInternalStorage aliceStorage =
                ASAPEngineFS.getASAPStorage(ALICE, ALICE_APP_FOLDER, CHAT_FORMAT);

        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE);
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE2);
        //aliceStorage.addRecipient(ALICE_BOB_CHAT_URL, BOB);

        // bob does the same
        ASAPInternalStorage bobStorage =
                ASAPEngineFS.getASAPStorage(BOB, BOB_APP_FOLDER, CHAT_FORMAT);

        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE);
        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE2);
        //bobStorage.addRecipient(ALICE_BOB_CHAT_URL, ALICE);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare multi engines                                  //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ASAPChunkReceivedTester aliceListener = new ASAPChunkReceivedTester();
        ASAPInternalPeer aliceEngine = ASAPInternalPeerFS.createASAPPeer(ALICE_ROOT_FOLDER, aliceListener);

        ASAPChunkReceivedTester bobListener = new ASAPChunkReceivedTester();
        ASAPInternalPeer bobEngine = ASAPInternalPeerFS.createASAPPeer(BOB_ROOT_FOLDER, bobListener);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        setup connection                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        int portNumber = this.getPortNumber();
        // create connections for both sides
        TCPStream aliceChannel = new TCPStream(portNumber, true, "a2b");
        TCPStream bobChannel = new TCPStream(portNumber, false, "b2a");

        aliceChannel.start();
        bobChannel.start();

        // wait to connect
        aliceChannel.waitForConnection();
        bobChannel.waitForConnection();

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        run asap connection                                    //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // run engine as thread
        ASAPPeerHandleConnectionThread aliceEngineThread = new ASAPPeerHandleConnectionThread(aliceEngine,
                aliceChannel.getInputStream(), aliceChannel.getOutputStream());

        aliceEngineThread.start();

        // and better debugging - no new thread
        bobEngine.handleConnection(bobChannel.getInputStream(), bobChannel.getOutputStream());

        // wait until communication probably ends
        System.out.flush();
        System.err.flush();
        Thread.sleep(2000);
        System.out.flush();
        System.err.flush();

        // close connections: note ASAPEngine does NOT close any connection!!
        aliceChannel.close();
        bobChannel.close();
        System.out.flush();
        System.err.flush();
        Thread.sleep(1000);
        System.out.flush();
        System.err.flush();

        // check results

        // listener must have been informed about new messages
        Assert.assertTrue(aliceListener.chunkReceived());
        Assert.assertTrue(bobListener.chunkReceived());


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                     open incoming storages                                    //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // get messages alice received
        ASAPChunkStorage aliceSenderStored =
                aliceStorage.getReceivedChunksStorage(aliceListener.getSender());

        ASAPInternalChunk aliceReceivedChunk =
                aliceSenderStored.getChunk(aliceListener.getUri(),
                        aliceListener.getEra());

        // #1
        Iterator<CharSequence> aliceReceivedMessages = aliceReceivedChunk.getMessagesAsCharSequence();
        CharSequence aliceReceivedMessage = aliceReceivedMessages.next();
        Assert.assertEquals(BOB2ALICE_MESSAGE, aliceReceivedMessage);
        // #2
        aliceReceivedMessage = aliceReceivedMessages.next();
        Assert.assertEquals(BOB2ALICE_MESSAGE2, aliceReceivedMessage);

        // get message bob received
        ASAPChunkStorage bobSenderStored =
                bobStorage.getReceivedChunksStorage(bobListener.getSender());

        ASAPInternalChunk bobReceivedChunk =
                bobSenderStored.getChunk(bobListener.getUri(),
                        bobListener.getEra());

        // #1
        Iterator<CharSequence> bobReceivedMessages = bobReceivedChunk.getMessagesAsCharSequence();
        CharSequence bobReceivedMessage = bobReceivedMessages.next();
        Assert.assertEquals(ALICE2BOB_MESSAGE, bobReceivedMessage);
        // #2
        bobReceivedMessage = bobReceivedMessages.next();
        Assert.assertEquals(ALICE2BOB_MESSAGE2, bobReceivedMessage);

        List<CharSequence> senderList = aliceStorage.getSender();
        // expect bob
        Assert.assertEquals(1, senderList.size());
        Assert.assertTrue(BOB.equalsIgnoreCase(senderList.get(0).toString()));

        // simulate a sync
        bobStorage = ASAPEngineFS.getASAPStorage(BOB, BOB_APP_FOLDER, CHAT_FORMAT);
        Assert.assertEquals(2, bobStorage.getEra());

        Assert.assertEquals(1, bobStorage.getChannelURIs().size());

        Thread.sleep(1000);
    }

}
