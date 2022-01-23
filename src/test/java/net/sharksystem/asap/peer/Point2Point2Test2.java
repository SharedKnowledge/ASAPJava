package net.sharksystem.asap.peer;

import net.sharksystem.TestConstants;
import net.sharksystem.TestHelper;
import net.sharksystem.asap.ASAP;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.engine.ASAPInternalChunk;
import net.sharksystem.asap.utils.PeerIDHelper;
import net.sharksystem.testsupport.ASAPTestPeerFS;
import net.sharksystem.testsupport.StoreReceivedMessages;
import net.sharksystem.utils.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

public class Point2Point2Test2 {
    public static final String WORKING_SUB_DIRECTORY = TestConstants.ROOT_DIRECTORY + "point2point2/";

    /**
     * An reenactment of a failed Android test - a subdirectory wasn't created or something..
     * @throws IOException
     * @throws ASAPException
     * @throws InterruptedException
     *
     */
    @Test
    public void point2point1() throws IOException, ASAPException, InterruptedException {
        TestHelper.removeFolder(WORKING_SUB_DIRECTORY);

        String appName = "shark/onlineExampleMessages";
        String uri = "asapExample://uriExample";
        byte[] message = "ASAP example message".getBytes(StandardCharsets.UTF_8);

        String aliceID = PeerIDHelper.createUniqueID();
        String bobID = PeerIDHelper.createUniqueID();
        String aliceDirectory = WORKING_SUB_DIRECTORY + "/" + aliceID;
        String bobDirectory = WORKING_SUB_DIRECTORY + "/" + bobID;

        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(appName);

        ///////////////// PEERS //////////////////////////////////////////////////////////////
        // setup mocked peer / asap application and activity in android
        ASAPTestPeerFS aliceSimplePeer = new ASAPTestPeerFS(aliceID, aliceDirectory, formats);

        ASAPTestPeerFS bobSimplePeer = new ASAPTestPeerFS(bobID, bobDirectory, formats);
        StoreReceivedMessages bobListener = new StoreReceivedMessages();
        bobSimplePeer.addASAPMessageReceivedListener(appName, bobListener);

        aliceSimplePeer.startEncounter(TestHelper.getPortNumber(), bobSimplePeer);
        // give your app a moment to process
        Thread.sleep(100);

        // send message
        System.out.println("\n>>>>>>>>>>>>>>>>>>> send message #1 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n");
        aliceSimplePeer.sendASAPMessage(appName, uri, message);
        Thread.sleep(100);

        Assert.assertEquals(1, bobListener.messageList.size());

        byte[] messageReceived = bobListener.messageList.get(0).getMessages().next();
        Assert.assertTrue(Utils.compareArrays(messageReceived, message));

        aliceSimplePeer.stopEncounter(bobSimplePeer);
        //Thread.sleep(Long.MAX_VALUE);
    }
}
