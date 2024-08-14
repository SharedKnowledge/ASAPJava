package net.sharksystem.asap.engine;

import net.sharksystem.fs.FSUtils;
import net.sharksystem.utils.testsupport.TestConstants;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPChunkStorage;
import net.sharksystem.asap.utils.ASAPChunkReceivedTester;
import net.sharksystem.asap.utils.ASAPPeerHandleConnectionThread;
import net.sharksystem.asap.cmdline.TCPStream;
import net.sharksystem.asap.utils.ASAPSerialization;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import static net.sharksystem.asap.engine.ASAPInternalPeer.DEFAULT_MAX_PROCESSING_TIME;

public class LongerMessages {
    public static final String ALICE_BOB_CHAT_URL = "content://aliceAndBob.talk";
    public static final String CHAT_FORMAT = "application/x-sn2-makan";
    public static String ALICE_ROOT_FOLDER = TestConstants.ROOT_DIRECTORY + "longmessage/Alice";
    public static String ALICE_APP_FOLDER = ALICE_ROOT_FOLDER + "/appFolder";
    public static final String BOB_ROOT_FOLDER = TestConstants.ROOT_DIRECTORY + "longmessage/Bob";
    public static final String BOB_APP_FOLDER = BOB_ROOT_FOLDER + "/appFolder";
    public static final String ALICE = "Alice";
    public static final String BOB = "Bob";


    // 200
    public static final String ALICE2BOB_MESSAGE = "11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111";
    //public static final String ALICE2BOB_MESSAGE = "Hi Bob";
    //public static final String ALICE2BOB_MESSAGE2 = "HiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgainHiYouAgain";
    public static final String ALICE2BOB_MESSAGE2 = "Hi Again";

    private static int portnumber = 7777;

    private int getPortNumber() {
        portnumber++;
        return portnumber;
    }

    @Test
    public void serializeDeserializeTest() throws IOException, ASAPException {
        long writeLong = 0x9ABCDEF012345678L;
        //long writeLong = 0x9999999999999999L;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASAPSerialization.writeLongParameter(writeLong, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        long readLong = ASAPSerialization.readLongParameter(bais);

        Assert.assertEquals(writeLong, readLong);
    }

    @Test
    public void longerMessagesAlice2Bob() throws IOException, ASAPException, InterruptedException {
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare storages                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        FSUtils.removeFolder(ALICE_ROOT_FOLDER); // clean previous version before
        FSUtils.removeFolder(BOB_ROOT_FOLDER); // clean previous version before

        // alice writes a message into chunkStorage
        ASAPInternalStorage aliceStorage = ASAPEngineFS.getASAPStorage(ALICE, ALICE_APP_FOLDER, CHAT_FORMAT);

        int aliceInitialEra = aliceStorage.getEra();

        aliceStorage.createChannel(ALICE_BOB_CHAT_URL, BOB);

        // content changed - next change in topology should increase alice era.
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE);

        // bob does the same
        ASAPInternalStorage bobStorage = ASAPEngineFS.getASAPStorage(BOB, BOB_APP_FOLDER, CHAT_FORMAT);

        int bobInitialEra = bobStorage.getEra();

        bobStorage.createChannel(ALICE_BOB_CHAT_URL, ALICE);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare multi engines                                  //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ASAPChunkReceivedTester aliceListener = new ASAPChunkReceivedTester();
        ASAPInternalPeer aliceEngine = ASAPInternalPeerFS.createASAPPeer(
                ALICE, ALICE_ROOT_FOLDER, DEFAULT_MAX_PROCESSING_TIME, aliceListener);

        ASAPChunkReceivedTester bobListener = new ASAPChunkReceivedTester();
        ASAPInternalPeer bobEngine = ASAPInternalPeerFS.createASAPPeer(
                BOB, BOB_ROOT_FOLDER, DEFAULT_MAX_PROCESSING_TIME, bobListener);

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
        //aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE2);

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
        Assert.assertTrue(bobListener.chunkReceived());


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                     open incoming storages                                   //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // get message bob received
        ASAPChunkStorage bobIncomingAliceStorage = bobStorage.getReceivedChunksStorage(ALICE);
        ASAPInternalChunk bobReceivedChunk = bobIncomingAliceStorage.getChunk(ALICE_BOB_CHAT_URL, aliceInitialEra);

        // #1
        Iterator<CharSequence> bobReceivedMessages = bobReceivedChunk.getMessagesAsCharSequence();
        CharSequence bobReceivedMessage = bobReceivedMessages.next();
        Assert.assertEquals(ALICE2BOB_MESSAGE, bobReceivedMessage);

        // #2 - in next era
        /*
        bobReceivedChunk = bobIncomingAliceStorage.getChunk(ALICE_BOB_CHAT_URL, ASAP.nextEra(aliceInitialEra));
        bobReceivedMessages = bobReceivedChunk.getMessagesAsCharSequence();
        bobReceivedMessage = bobReceivedMessages.next();
        Assert.assertEquals(ALICE2BOB_MESSAGE2, bobReceivedMessage);

         */

        Thread.sleep(1000);
    }

}
