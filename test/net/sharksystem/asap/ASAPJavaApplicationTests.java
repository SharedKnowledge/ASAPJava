package net.sharksystem.asap;

import net.sharksystem.asap.apps.*;
import net.sharksystem.cmdline.TCPStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

public class ASAPJavaApplicationTests {
    public static final String ALICE = "Alice";
    public static final String BOB = "Bob";
    public static final String TESTS_ROOT_FOLDER = "tests/";
    public static final String ALICE_ROOT_FOLDER = TESTS_ROOT_FOLDER + "Alice";
    public static final String BOB_ROOT_FOLDER = TESTS_ROOT_FOLDER + "Bob";
    private static final CharSequence APP_FORMAT = "TEST_FORMAT";
    private static final byte[] TESTMESSAGE = "TestMessage".getBytes();
    private static final int PORT = 7777;

    @Test
    public void usageTest() throws IOException, ASAPException, InterruptedException {
        ASAPEngineFS.removeFolder(TESTS_ROOT_FOLDER);
        Collection<CharSequence> formats = new HashSet<>();
        formats.add(APP_FORMAT);

        ASAPJavaApplication asapJavaApplicationAlice =
                ASAPJavaApplicationFS.createASAPJavaApplication(ALICE, ALICE_ROOT_FOLDER, formats);

        Collection<CharSequence> recipients = new HashSet<>();
        recipients.add(BOB);

        asapJavaApplicationAlice.sendASAPMessage(APP_FORMAT, "yourSchema://yourURI", recipients, TESTMESSAGE);
        asapJavaApplicationAlice.setASAPMessageReceivedListener(APP_FORMAT, new ListenerExample());

        // create bob engine
        ASAPJavaApplication asapJavaApplicationBob =
                ASAPJavaApplicationFS.createASAPJavaApplication(BOB, BOB_ROOT_FOLDER, formats);

        ListenerExample listenerBob = new ListenerExample();
        asapJavaApplicationBob.setASAPMessageReceivedListener(APP_FORMAT, listenerBob);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        create a tcp connection                                //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // create connections for both sides
        TCPStream aliceChannel = new TCPStream(PORT, true, "a2b");
        TCPStream bobChannel = new TCPStream(PORT, false, "b2a");

        aliceChannel.start(); bobChannel.start();
        // wait to connect
        aliceChannel.waitForConnection(); bobChannel.waitForConnection();

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        run asap session                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // run engine as thread
        ASAPHandleConnectionThread aliceEngineThread = new ASAPHandleConnectionThread(asapJavaApplicationAlice,
                aliceChannel.getInputStream(), aliceChannel.getOutputStream());

        aliceEngineThread.start();

        // let's start communication
        asapJavaApplicationBob.handleConnection(bobChannel.getInputStream(), bobChannel.getOutputStream());

        // wait until communication probably ends
        Thread.sleep(2000); System.out.flush(); System.err.flush();
        // close connections: note ASAPEngine does NOT close any connection!!
        aliceChannel.close(); bobChannel.close(); Thread.sleep(1000);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                            test results                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // received?
        Assert.assertTrue(listenerBob.hasReceivedMessage());
    }

    @Test
    public void onlineTest() throws IOException, ASAPException, InterruptedException {
        ASAPEngineFS.removeFolder(TESTS_ROOT_FOLDER);

        Collection<CharSequence> formats = new HashSet<>();
        formats.add(APP_FORMAT);

        // create alice engine
        ASAPJavaApplication asapJavaApplicationAlice =
                ASAPJavaApplicationFS.createASAPJavaApplication(ALICE, ALICE_ROOT_FOLDER, formats);

        // create bob engine
        ASAPJavaApplication asapJavaApplicationBob =
                ASAPJavaApplicationFS.createASAPJavaApplication(BOB, BOB_ROOT_FOLDER, formats);

        // create Bob receiver
        ListenerExample listenerBob = new ListenerExample();
        asapJavaApplicationBob.setASAPMessageReceivedListener(APP_FORMAT, listenerBob);


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        create a tcp connection                                //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // create connections for both sides
        TCPStream aliceChannel = new TCPStream(PORT, true, "a2b");
        TCPStream bobChannel = new TCPStream(PORT, false, "b2a");

        aliceChannel.start(); bobChannel.start();
        // wait to connect
        aliceChannel.waitForConnection(); bobChannel.waitForConnection();

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        run asap session                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // run engine as thread
        ASAPHandleConnectionThread aliceEngineThread = new ASAPHandleConnectionThread(asapJavaApplicationAlice,
                aliceChannel.getInputStream(), aliceChannel.getOutputStream());

        aliceEngineThread.start();

        // let's start communication
        asapJavaApplicationBob.handleConnection(bobChannel.getInputStream(), bobChannel.getOutputStream());

        // nothing really spectaculare has happend until now

        // create a message and send message from alice to bob over an existing connection
        Collection<CharSequence> recipients = new HashSet<>();
        recipients.add(BOB);
      asapJavaApplicationAlice.sendASAPMessage(APP_FORMAT, "yourSchema://yourURI", recipients, TESTMESSAGE);

        // wait until communication probably ends
        Thread.sleep(2000); System.out.flush(); System.err.flush();
        // close connections: note ASAPEngine does NOT close any connection!!
        aliceChannel.close(); bobChannel.close(); Thread.sleep(1000);

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                            test results                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // received?
        Assert.assertTrue(listenerBob.hasReceivedMessage());
    }

    private class ListenerExample implements ASAPMessageReceivedListener {

        private boolean hasReceivedMessage = false;

        @Override
        public void asapMessagesReceived(ASAPMessages messages) {
            try {
                System.out.println("#message == " + messages.size());
                this.hasReceivedMessage = true;
            } catch (IOException e) {
                // do something with it.
            }
        }

        public boolean hasReceivedMessage() {
            return this.hasReceivedMessage;
        }
    }
}
