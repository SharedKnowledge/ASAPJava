package net.sharksystem.asap.engine;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import net.sharksystem.TestConstants;
import net.sharksystem.asap.ASAP;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPChunkStorage;
import net.sharksystem.asap.utils.ASAPChunkReceivedTester;
import net.sharksystem.asap.utils.ASAPPeerHandleConnectionThread;
import net.sharksystem.asap.cmdline.TCPStream;
import org.junit.Test;
import org.junit.Assert;

import static net.sharksystem.TestConstants.*;
import static net.sharksystem.asap.engine.ASAPInternalPeer.DEFAULT_MAX_PROCESSING_TIME;

/**
 * Here are some basic tests and usage examples.
 * @author thsc
 */
public class Point2PointTests {
    private static final String TEST_CLASS_ROOT_FOLDER = TestConstants.ROOT_DIRECTORY + Point2PointTests.class.getSimpleName() + "/";
    public static final String ALICE_BOB_CHAT_URL = "content://aliceAndBob.talk";
    public static final String CHAT_FORMAT = "application/x-sn2-makan";
    public static final String ALICE_ROOT_FOLDER = TEST_CLASS_ROOT_FOLDER + ALICE_NAME;
    public static final String ALICE_APP_FOLDER = ALICE_ROOT_FOLDER + "/appFolder";
    public static final String BOB_ROOT_FOLDER = TEST_CLASS_ROOT_FOLDER + BOB_NAME;
    public static final String BOB_APP_FOLDER = BOB_ROOT_FOLDER + "/appFolder";
    public static final String ALICE2BOB_MESSAGE = "Hi Bob";
    // 400 characters
    //public static final String ALICE2BOB_MESSAGE2 = "HiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgain";
    public static final String ALICE2BOB_MESSAGE2 = "Hi You Again";
    public static final String BOB2ALICE_MESSAGE = "Hi Alice";
    public static final String BOB2ALICE_MESSAGE2 = "Hi Alice again";

    private static int portnumber = 7777;

    private int getPortNumber() {
        portnumber++;
        return portnumber;
    }

    @Test
    public void notOpenMessageChunkExchange() throws IOException, ASAPException, InterruptedException {
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare storages                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ASAPEngineFS.removeFolder(ALICE_ROOT_FOLDER); // clean previous version before
        ASAPEngineFS.removeFolder(BOB_ROOT_FOLDER); // clean previous version before

        // alice writes a message into chunkStorage
        ASAPInternalStorage aliceStorage =
                ASAPEngineFS.getASAPStorage(ALICE_NAME, ALICE_APP_FOLDER, CHAT_FORMAT);

        aliceStorage.createChannel(ALICE_BOB_CHAT_URL, BOB_NAME); // Add recipient: make it a non-open channel
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE);
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE2);

        // bob does the same
        ASAPInternalStorage bobStorage =
                ASAPEngineFS.getASAPStorage(BOB_NAME, BOB_APP_FOLDER, CHAT_FORMAT);

        int bobInitialEra = bobStorage.getEra();

        bobStorage.createChannel(ALICE_BOB_CHAT_URL, ALICE_NAME); // Add recipient: make it a non-open channel
        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE);
        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE2);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare multi engines                                  //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ASAPChunkReceivedTester aliceListener = new ASAPChunkReceivedTester();
        ASAPInternalPeer aliceEngine = ASAPInternalPeerFS.createASAPPeer(
                ALICE_NAME, ALICE_ROOT_FOLDER, DEFAULT_MAX_PROCESSING_TIME, aliceListener);

        ASAPChunkReceivedTester bobListener = new ASAPChunkReceivedTester();
        ASAPInternalPeer bobEngine = ASAPInternalPeerFS.createASAPPeer(
                BOB_NAME, BOB_ROOT_FOLDER, DEFAULT_MAX_PROCESSING_TIME, bobListener);

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
        //                                     open incoming storages                                   //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // get messages alice received
        ASAPChunkStorage aliceSenderStored =
                aliceStorage.getReceivedChunksStorage(aliceListener.getSenderE2E());

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
                bobStorage.getReceivedChunksStorage(bobListener.getSenderE2E());

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
        Assert.assertTrue(BOB_NAME.equalsIgnoreCase(senderList.get(0).toString()));

        // simulate a sync
        bobStorage = ASAPEngineFS.getASAPStorage(BOB_NAME, BOB_APP_FOLDER, CHAT_FORMAT);
        Assert.assertEquals(bobInitialEra+2, bobStorage.getEra());

        Thread.sleep(1000);
    }

    @Test
    public void usageWithImmediateSync() throws IOException, ASAPException, InterruptedException {
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare storages                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ASAPEngineFS.removeFolder(ALICE_ROOT_FOLDER); // clean previous version before
        ASAPEngineFS.removeFolder(BOB_ROOT_FOLDER); // clean previous version before

        // alice writes a message into chunkStorage
        ASAPInternalStorage aliceStorage = ASAPEngineFS.getASAPStorage(ALICE_NAME, ALICE_APP_FOLDER, CHAT_FORMAT);

        int aliceInitialEra = aliceStorage.getEra();

        aliceStorage.createChannel(ALICE_BOB_CHAT_URL, BOB_NAME);

        // content changed - next change in topology should increase alice era.
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE);

        // bob does the same
        ASAPInternalStorage bobStorage = ASAPEngineFS.getASAPStorage(BOB_NAME, BOB_APP_FOLDER, CHAT_FORMAT);

        int bobInitialEra = bobStorage.getEra();

        bobStorage.createChannel(ALICE_BOB_CHAT_URL, ALICE_NAME);

        // content changed - next change in topology should increase bob era.
        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare multi engines                                  //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ASAPChunkReceivedTester aliceListener = new ASAPChunkReceivedTester();
        ASAPInternalPeer aliceEngine = ASAPInternalPeerFS.createASAPPeer(
                ALICE_NAME, ALICE_ROOT_FOLDER, DEFAULT_MAX_PROCESSING_TIME, aliceListener);

        ASAPChunkReceivedTester bobListener = new ASAPChunkReceivedTester();
        ASAPInternalPeer bobEngine = ASAPInternalPeerFS.createASAPPeer(
                BOB_NAME, BOB_ROOT_FOLDER, DEFAULT_MAX_PROCESSING_TIME, bobListener);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                       prepare asap immediate bypass                           //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ASAPAbstractOnlineMessageSender aliceBypass = new ASAPSingleProcessOnlineMessageSender(aliceEngine, aliceStorage);
        ASAPAbstractOnlineMessageSender bobBypass = new ASAPSingleProcessOnlineMessageSender(bobEngine, bobStorage);

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

        // give handleConnection some time.
        Thread.sleep(1000);
        // create second message after creating a connection - should be bypassed.
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE2);
        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE2);

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
        //                                     open incoming storages                                   //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // get messages alice received
        ASAPChunkStorage aliceIncomingBobStorage = aliceStorage.getReceivedChunksStorage(BOB_NAME);
        ASAPInternalChunk aliceReceivedChunk = aliceIncomingBobStorage.getChunk(ALICE_BOB_CHAT_URL, bobInitialEra);

        // must be one message
        Iterator<CharSequence> aliceReceivedMessages = aliceReceivedChunk.getMessagesAsCharSequence();
        CharSequence aliceReceivedMessage = aliceReceivedMessages.next();
        Assert.assertEquals(BOB2ALICE_MESSAGE, aliceReceivedMessage);

        // #2 is in another era
        aliceReceivedChunk = aliceIncomingBobStorage.getChunk(ALICE_BOB_CHAT_URL, ASAP.nextEra(bobInitialEra));
        aliceReceivedMessages = aliceReceivedChunk.getMessagesAsCharSequence();
        aliceReceivedMessage = aliceReceivedMessages.next();
        Assert.assertEquals(BOB2ALICE_MESSAGE2, aliceReceivedMessage);

        // get message bob received
        ASAPChunkStorage bobIncomingAliceStorage = bobStorage.getReceivedChunksStorage(ALICE_NAME);
        ASAPInternalChunk bobReceivedChunk = bobIncomingAliceStorage.getChunk(ALICE_BOB_CHAT_URL, aliceInitialEra);

        // #1
        Iterator<CharSequence> bobReceivedMessages = bobReceivedChunk.getMessagesAsCharSequence();
        CharSequence bobReceivedMessage = bobReceivedMessages.next();
        Assert.assertEquals(ALICE2BOB_MESSAGE, bobReceivedMessage);

        // #2 - in next era
        bobReceivedChunk = bobIncomingAliceStorage.getChunk(ALICE_BOB_CHAT_URL, ASAP.nextEra(aliceInitialEra));
        bobReceivedMessages = bobReceivedChunk.getMessagesAsCharSequence();
        bobReceivedMessage = bobReceivedMessages.next();
        Assert.assertEquals(ALICE2BOB_MESSAGE2, bobReceivedMessage);

        Thread.sleep(1000);
    }

    @Test
    public void killOpenConnection() throws IOException, ASAPException, InterruptedException {
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare storages                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ASAPEngineFS.removeFolder(ALICE_ROOT_FOLDER); // clean previous version before
        ASAPEngineFS.removeFolder(BOB_ROOT_FOLDER); // clean previous version before

        // alice writes a message into chunkStorage
        ASAPInternalStorage aliceStorage =
                ASAPEngineFS.getASAPStorage(ALICE_NAME, ALICE_APP_FOLDER, CHAT_FORMAT);

        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE);
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE2);
        //aliceStorage.addRecipient(ALICE_BOB_CHAT_URL, BOB);

        // bob does the same
        ASAPInternalStorage bobStorage =
                ASAPEngineFS.getASAPStorage(BOB_NAME, BOB_APP_FOLDER, CHAT_FORMAT);

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
/*
        ASAPEngineThread aliceEngineThread = new ASAPEngineThread(aliceEngine,
                aliceChannel.getInputStream(), aliceChannel.getOutputStream());

        aliceEngineThread.start();
 */
        // and better debugging - no new thread
        bobEngine.handleConnection(bobChannel.getInputStream(), bobChannel.getOutputStream());

        // wait until communication probably ends
        Thread.sleep(1000);

        // kill connection on bob side
        bobChannel.close();

        Thread.sleep(1000);
        System.out.flush();
        System.err.flush();

        // check results
    }
}
