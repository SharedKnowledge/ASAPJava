package net.sharksystem.asap;

import net.sharksystem.asap.apps.*;
import net.sharksystem.asap.util.ASAPEngineThread;
import net.sharksystem.cmdline.TCPChannel;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

public class ASAPJavaApplicationTests {
    public static final String ALICE = "Alice";
    public static final String BOB = "Bob";
    public static final String ALICE_ROOT_FOLDER = "tests/Alice";
    public static final String BOB_ROOT_FOLDER = "tests/Bob";
    private static final CharSequence APP_FORMAT = "TEST_FORMAT";
    private static final byte[] TESTMESSAGE = "TestMessage".getBytes();
    private static final int PORT = 7777;

    @Test
    public void usageTest() throws IOException, ASAPException, InterruptedException {
        Collection<CharSequence> formats = new HashSet<>();
        formats.add(APP_FORMAT);

        ASAPJavaApplication asapJavaApplicationAlice =
                ASAPJavaApplicationFS.createASAPJavaApplication(ALICE, ALICE_ROOT_FOLDER, formats);

        Collection<CharSequence> recipients = new HashSet<>();
        recipients.add(BOB);

        asapJavaApplicationAlice.sendASAPMessage(APP_FORMAT, "yourSchema://yourURI", recipients, TESTMESSAGE);
        asapJavaApplicationAlice.setASAPMessageReceivedListener(APP_FORMAT, new ListenerExample());

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        create a tcp connection                                //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // create bob engine
        ASAPJavaApplication asapJavaApplicationBob =
                ASAPJavaApplicationFS.createASAPJavaApplication(BOB, BOB_ROOT_FOLDER, formats);

        asapJavaApplicationBob.setASAPMessageReceivedListener(APP_FORMAT, new ListenerExample());

        // create connections for both sides
        TCPChannel aliceChannel = new TCPChannel(PORT, true, "a2b");
        TCPChannel bobChannel = new TCPChannel(PORT, false, "b2a");

        aliceChannel.start();
        bobChannel.start();

        // wait to connect
        aliceChannel.waitForConnection();
        bobChannel.waitForConnection();

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        run asap connection                                    //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // run engine as thread
        ASAPHandleConnectionThread aliceEngineThread = new ASAPHandleConnectionThread(asapJavaApplicationAlice,
                aliceChannel.getInputStream(), aliceChannel.getOutputStream());

        aliceEngineThread.start();

        // let's start communication
        asapJavaApplicationBob.handleConnection(bobChannel.getInputStream(), bobChannel.getOutputStream());

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

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                            test results                                       //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // TODO: should test something
    }

    private class ListenerExample implements ASAPMessageReceivedListener {

        @Override
        public void asapMessagesReceived(ASAPMessages messages) {
            try {
                System.out.println("#message == " + messages.size());
            } catch (IOException e) {
                // do something with it.
            }
        }
    }
}
