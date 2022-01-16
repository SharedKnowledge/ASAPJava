package net.sharksystem.asap.peer;

import net.sharksystem.CountsReceivedMessagesListener;
import net.sharksystem.TestConstants;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.apps.testsupport.ASAPTestPeerFS;
import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class TransientMessages {
    public static final String WORKING_SUB_DIRECTORY = TestConstants.ROOT_DIRECTORY + "transientMessages/";
    public static final String APPNAME = "asap/transient";

    public void exchangeTransientMessage() throws IOException, ASAPException, InterruptedException {
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(APPNAME);

        ///////////////// ALICE //////////////////////////////////////////////////////////////
        // setup mocked peer / asap application and activity in android
        ASAPTestPeerFS aliceSimplePeer = new ASAPTestPeerFS(TestConstants.ALICE_ID, formats);
        CountsReceivedMessagesListener aliceListener = new CountsReceivedMessagesListener(TestConstants.ALICE_ID);
        aliceSimplePeer.addASAPMessageReceivedListener(APPNAME, aliceListener);

        ASAPTestPeerFS bobSimplePeer = new ASAPTestPeerFS(TestConstants.BOB_ID, formats);
        CountsReceivedMessagesListener bobListener = new CountsReceivedMessagesListener(TestConstants.ALICE_ID);
        bobSimplePeer.addASAPMessageReceivedListener(APPNAME, bobListener);

        aliceSimplePeer.sendTransientASAPMessage(APPNAME, TestConstants.URI, TestConstants.MESSAGE_ALICE_TO_BOB_1);

        bobSimplePeer.sendTransientASAPMessage(APPNAME, TestConstants.URI, TestConstants.MESSAGE_BOB_TO_ALICE_1);

        aliceSimplePeer.startEncounter(7777, bobSimplePeer);
        // give your app a moment to process
        Thread.sleep(100);
        // stop encounter
        bobSimplePeer.stopEncounter(aliceSimplePeer);
        // give your app a moment to process
        Thread.sleep(100);

        Assert.assertEquals(1, aliceListener.numberOfMessages);
        Assert.assertEquals(1, bobListener.numberOfMessages);
    }
}
