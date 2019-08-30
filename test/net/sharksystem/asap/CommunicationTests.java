package net.sharksystem.asap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sharksystem.asap.util.ImmediateASAPMessageTransfer;
import net.sharksystem.util.localloop.TCPChannel;
import org.junit.Test;
import org.junit.Assert;

/**
 * Here are some basic tests and usage examples.
 * @author thsc
 */
public class CommunicationTests {
    public static final String ALICE_BOB_CHAT_URL = "content://aliceAndBob.talk";
    public static final String CHAT_FORMAT = "application/x-sn2-makan";
    public static final String ALICE_ROOT_FOLDER = "tests/alice";
    public static final String ALICE_APP_FOLDER = ALICE_ROOT_FOLDER + "/appFolder";
    public static final String BOB_ROOT_FOLDER = "tests/bob";
    public static final String BOB_APP_FOLDER = BOB_ROOT_FOLDER + "/appFolder";
    public static final String ALICE = "alice";
    public static final String BOB = "bob";
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
    public void undisclosedMessageChunkExchange() throws IOException, ASAPException, InterruptedException {
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare storages                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ASAPEngineFS.removeFolder(ALICE_ROOT_FOLDER); // clean previous version before
        ASAPEngineFS.removeFolder(BOB_ROOT_FOLDER); // clean previous version before

        // alice writes a message into chunkStorage
        ASAPStorage aliceStorage =
                ASAPEngineFS.getASAPStorage(ALICE, ALICE_APP_FOLDER, CHAT_FORMAT);

        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE);
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE2);
        //aliceStorage.addRecipient(ALICE_BOB_CHAT_URL, BOB);

        // bob does the same
        ASAPStorage bobStorage =
                ASAPEngineFS.getASAPStorage(BOB, BOB_APP_FOLDER, CHAT_FORMAT);

        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE);
        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE2);
        //bobStorage.addRecipient(ALICE_BOB_CHAT_URL, ALICE);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare multi engines                                  //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ASAPChunkReceiverTester aliceListener = new ASAPChunkReceiverTester();
        MultiASAPEngineFS aliceEngine = MultiASAPEngineFS_Impl.createMultiEngine(ALICE_ROOT_FOLDER, aliceListener);

        ASAPChunkReceiverTester bobListener = new ASAPChunkReceiverTester();
        MultiASAPEngineFS bobEngine = MultiASAPEngineFS_Impl.createMultiEngine(BOB_ROOT_FOLDER, bobListener);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        setup connection                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        int portNumber = this.getPortNumber();
        // create connections for both sides
        TCPChannel aliceChannel = new TCPChannel(portNumber, true, "a2b");
        TCPChannel bobChannel = new TCPChannel(portNumber, false, "b2a");

        aliceChannel.start();
        bobChannel.start();

        // wait to connect
        aliceChannel.waitForConnection();
        bobChannel.waitForConnection();

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        run asap connection                                    //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // run engine as thread
        ASAPEngineThread aliceEngineThread = new ASAPEngineThread(aliceEngine,
                aliceChannel.getInputStream(), aliceChannel.getOutputStream());

        aliceEngineThread.start();

        // and better debugging - no new thread
        bobEngine.handleConnection(bobChannel.getInputStream(), bobChannel.getOutputStream());

        // wait until communication probably ends
        System.out.flush();
        System.err.flush();
        Thread.sleep(5000);
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
        //                                     open incomming storages                                   //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // get messages alice received
        ASAPChunkStorage aliceSenderStored =
                aliceStorage.getIncomingChunkStorage(aliceListener.getSender());

        ASAPChunk aliceReceivedChunk =
                aliceSenderStored.getChunk(aliceListener.getUri(),
                        aliceListener.getEra());

        // #1
        Iterator<CharSequence> aliceReceivedMessages = aliceReceivedChunk.getMessages();
        CharSequence aliceReceivedMessage = aliceReceivedMessages.next();
        Assert.assertEquals(BOB2ALICE_MESSAGE, aliceReceivedMessage);
        // #2
        aliceReceivedMessage = aliceReceivedMessages.next();
        Assert.assertEquals(BOB2ALICE_MESSAGE2, aliceReceivedMessage);

        // get message bob received
        ASAPChunkStorage bobSenderStored =
                bobStorage.getIncomingChunkStorage(bobListener.getSender());

        ASAPChunk bobReceivedChunk =
                bobSenderStored.getChunk(bobListener.getUri(),
                        bobListener.getEra());

        // #1
        Iterator<CharSequence> bobReceivedMessages = bobReceivedChunk.getMessages();
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
        Assert.assertEquals(1, bobStorage.getEra());
    }
    @Test
    public void notOpenMessageChunkExchange() throws IOException, ASAPException, InterruptedException {
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare storages                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ASAPEngineFS.removeFolder(ALICE_ROOT_FOLDER); // clean previous version before
        ASAPEngineFS.removeFolder(BOB_ROOT_FOLDER); // clean previous version before

        // alice writes a message into chunkStorage
        ASAPStorage aliceStorage =
                ASAPEngineFS.getASAPStorage(ALICE, ALICE_APP_FOLDER, CHAT_FORMAT);

        aliceStorage.addRecipient(ALICE_BOB_CHAT_URL, BOB); // Add recipient: make it a non-open channel
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE);
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE2);

        // bob does the same
        ASAPStorage bobStorage =
                ASAPEngineFS.getASAPStorage(BOB, BOB_APP_FOLDER, CHAT_FORMAT);

        bobStorage.addRecipient(ALICE_BOB_CHAT_URL, ALICE); // Add recipient: make it a non-open channel
        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE);
        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE2);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare multi engines                                  //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ASAPChunkReceiverTester aliceListener = new ASAPChunkReceiverTester();
        MultiASAPEngineFS aliceEngine = MultiASAPEngineFS_Impl.createMultiEngine(ALICE_ROOT_FOLDER, aliceListener);

        ASAPChunkReceiverTester bobListener = new ASAPChunkReceiverTester();
        MultiASAPEngineFS bobEngine = MultiASAPEngineFS_Impl.createMultiEngine(BOB_ROOT_FOLDER, bobListener);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        setup connection                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        int portNumber = this.getPortNumber();
        // create connections for both sides
        TCPChannel aliceChannel = new TCPChannel(portNumber, true, "a2b");
        TCPChannel bobChannel = new TCPChannel(portNumber, false, "b2a");

        aliceChannel.start();
        bobChannel.start();

        // wait to connect
        aliceChannel.waitForConnection();
        bobChannel.waitForConnection();

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        run asap connection                                    //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // run engine as thread
        ASAPEngineThread aliceEngineThread = new ASAPEngineThread(aliceEngine,
                aliceChannel.getInputStream(), aliceChannel.getOutputStream());

        aliceEngineThread.start();

        // and better debugging - no new thread
        bobEngine.handleConnection(bobChannel.getInputStream(), bobChannel.getOutputStream());

        // wait until communication probably ends
        System.out.flush();
        System.err.flush();
        Thread.sleep(5000);
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
        //                                     open incomming storages                                   //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // get messages alice received
        ASAPChunkStorage aliceSenderStored =
                aliceStorage.getIncomingChunkStorage(aliceListener.getSender());

        ASAPChunk aliceReceivedChunk =
                aliceSenderStored.getChunk(aliceListener.getUri(),
                        aliceListener.getEra());

        // #1
        Iterator<CharSequence> aliceReceivedMessages = aliceReceivedChunk.getMessages();
        CharSequence aliceReceivedMessage = aliceReceivedMessages.next();
        Assert.assertEquals(BOB2ALICE_MESSAGE, aliceReceivedMessage);
        // #2
        aliceReceivedMessage = aliceReceivedMessages.next();
        Assert.assertEquals(BOB2ALICE_MESSAGE2, aliceReceivedMessage);

        // get message bob received
        ASAPChunkStorage bobSenderStored =
                bobStorage.getIncomingChunkStorage(bobListener.getSender());

        ASAPChunk bobReceivedChunk =
                bobSenderStored.getChunk(bobListener.getUri(),
                        bobListener.getEra());

        // #1
        Iterator<CharSequence> bobReceivedMessages = bobReceivedChunk.getMessages();
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
        Assert.assertEquals(1, bobStorage.getEra());
    }

    @Test
    public void usageWithImmediateSync() throws IOException, ASAPException, InterruptedException {
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare storages                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ASAPEngineFS.removeFolder(ALICE_ROOT_FOLDER); // clean previous version before
        ASAPEngineFS.removeFolder(BOB_ROOT_FOLDER); // clean previous version before

        // alice writes a message into chunkStorage
        ASAPStorage aliceStorage =
                ASAPEngineFS.getASAPStorage(ALICE, ALICE_APP_FOLDER, CHAT_FORMAT);

        aliceStorage.addRecipient(ALICE_BOB_CHAT_URL, BOB);
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE);

        // bob does the same
        ASAPStorage bobStorage =
                ASAPEngineFS.getASAPStorage(BOB, BOB_APP_FOLDER, CHAT_FORMAT);

        bobStorage.addRecipient(ALICE_BOB_CHAT_URL, ALICE);
        bobStorage.add(ALICE_BOB_CHAT_URL, BOB2ALICE_MESSAGE);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare multi engines                                  //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ASAPChunkReceiverTester aliceListener = new ASAPChunkReceiverTester();
        MultiASAPEngineFS aliceEngine = MultiASAPEngineFS_Impl.createMultiEngine(ALICE_ROOT_FOLDER, aliceListener);

        ASAPChunkReceiverTester bobListener = new ASAPChunkReceiverTester();
        MultiASAPEngineFS bobEngine = MultiASAPEngineFS_Impl.createMultiEngine(BOB_ROOT_FOLDER, bobListener);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                       prepare asap immediate bypass                           //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ImmediateASAPMessageTransfer aliceBypass = new ImmediateASAPMessageTransfer(aliceEngine, aliceStorage);
        ImmediateASAPMessageTransfer bobBypass = new ImmediateASAPMessageTransfer(bobEngine, bobStorage);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        setup connection                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        int portNumber = this.getPortNumber();
        // create connections for both sides
        TCPChannel aliceChannel = new TCPChannel(portNumber, true, "a2b");
        TCPChannel bobChannel = new TCPChannel(portNumber, false, "b2a");

        aliceChannel.start();
        bobChannel.start();

        // wait to connect
        aliceChannel.waitForConnection();
        bobChannel.waitForConnection();

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        run asap connection                                    //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // run engine as thread
        ASAPEngineThread aliceEngineThread = new ASAPEngineThread(aliceEngine,
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
        Thread.sleep(5000);
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
        //                                     open incomming storages                                   //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // get messages alice received
        ASAPChunkStorage aliceSenderStored =
                aliceStorage.getIncomingChunkStorage(aliceListener.getSender());

        ASAPChunk aliceReceivedChunk =
                aliceSenderStored.getChunk(aliceListener.getUri(),
                        aliceListener.getEra());

        // #1
        Iterator<CharSequence> aliceReceivedMessages = aliceReceivedChunk.getMessages();
        CharSequence aliceReceivedMessage = aliceReceivedMessages.next();
        Assert.assertEquals(BOB2ALICE_MESSAGE, aliceReceivedMessage);
        // #2
        aliceReceivedMessage = aliceReceivedMessages.next();
        Assert.assertEquals(BOB2ALICE_MESSAGE2, aliceReceivedMessage);

        // get message bob received
        ASAPChunkStorage bobSenderStored =
                bobStorage.getIncomingChunkStorage(bobListener.getSender());

        ASAPChunk bobReceivedChunk =
                bobSenderStored.getChunk(bobListener.getUri(),
                        bobListener.getEra());

        // #1
        Iterator<CharSequence> bobReceivedMessages = bobReceivedChunk.getMessages();
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
        Assert.assertEquals(1, bobStorage.getEra());
    }
}
