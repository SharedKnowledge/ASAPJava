package net.sharksystem.asap;

import net.sharksystem.asap.*;
import net.sharksystem.asap.util.ASAPChunkReceivedTester;
import net.sharksystem.asap.util.ASAPPeerHandleConnectionThread;
import net.sharksystem.cmdline.TCPStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class CreateNewChannelFromOutsideTest {
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
        ASAPStorage aliceStorage =
                ASAPEngineFS.getASAPStorage(ALICE, ALICE_APP_FOLDER, CHAT_FORMAT);

        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE);
        aliceStorage.add(ALICE_BOB_CHAT_URL, ALICE2BOB_MESSAGE2);
        //aliceStorage.addRecipient(ALICE_BOB_CHAT_URL, BOB);

        // bob does the same
        ASAPStorage bobStorage =
                ASAPEngineFS.getASAPStorage(BOB, BOB_APP_FOLDER, CHAT_FORMAT);

        // there is only Bobs storage - nothing else. That's important.

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                        prepare peers                                          //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        ASAPPeer aliceEngine = ASAPPeerFS.createASAPPeer(ALICE_ROOT_FOLDER, null);
        ASAPPeer bobEngine = ASAPPeerFS.createASAPPeer(BOB_ROOT_FOLDER, null);

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

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        //                                             tests                                             //
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // there is a new channel
        List<CharSequence> channelURIs = bobStorage.getChannelURIs();
        Assert.assertEquals(1, channelURIs.size());

        // and the one and only is...
        Assert.assertEquals(ALICE_BOB_CHAT_URL, channelURIs.get(0));
    }
}
