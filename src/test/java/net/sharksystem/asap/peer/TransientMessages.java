package net.sharksystem.asap.peer;

import net.sharksystem.CountsReceivedMessagesListener;
import net.sharksystem.TestConstants;
import net.sharksystem.TestHelper;
import net.sharksystem.asap.ASAP;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.engine.ASAPInternalChunk;
import net.sharksystem.testsupport.ASAPTestPeerFS;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

public class TransientMessages {
    public static final String WORKING_SUB_DIRECTORY = TestConstants.ROOT_DIRECTORY + "transientMessages/";
    public static final String ALICE_DIRECTORY = WORKING_SUB_DIRECTORY + "/" + TestConstants.ALICE_NAME;
    public static final String BOB_DIRECTORY = WORKING_SUB_DIRECTORY + "/" + TestConstants.BOB_NAME;

    public static final String APPNAME = "asap/transient";

    @Test
    public void exchangeTransientMessage() throws IOException, ASAPException, InterruptedException {
        TestHelper.removeFolder(WORKING_SUB_DIRECTORY);
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(APPNAME);

        ///////////////// ALICE //////////////////////////////////////////////////////////////
        // setup mocked peer / asap application and activity in android
        ASAPTestPeerFS aliceSimplePeer = new ASAPTestPeerFS(TestConstants.ALICE_ID, ALICE_DIRECTORY, formats);
        CountsReceivedMessagesListener aliceListener = new CountsReceivedMessagesListener(TestConstants.ALICE_ID);
        aliceSimplePeer.addASAPMessageReceivedListener(APPNAME, aliceListener);

        ASAPTestPeerFS bobSimplePeer = new ASAPTestPeerFS(TestConstants.BOB_ID, BOB_DIRECTORY, formats);
        CountsReceivedMessagesListener bobListener = new CountsReceivedMessagesListener(TestConstants.ALICE_ID);
        bobSimplePeer.addASAPMessageReceivedListener(APPNAME, bobListener);

        aliceSimplePeer.startEncounter(7777, bobSimplePeer);
        // give your app a moment to process
        Thread.sleep(100);

        // exchange transient messages
        aliceSimplePeer.sendTransientASAPMessage(APPNAME, TestConstants.URI, TestConstants.MESSAGE_ALICE_TO_BOB_1);
        bobSimplePeer.sendTransientASAPMessage(APPNAME, TestConstants.URI, TestConstants.MESSAGE_BOB_TO_ALICE_1);
        Thread.sleep(500);

        // stop encounter
        bobSimplePeer.stopEncounter(aliceSimplePeer);
        // give your app a moment to process
        Thread.sleep(100);

        Assert.assertEquals(1, aliceListener.numberOfMessages);
        Assert.assertEquals(1, bobListener.numberOfMessages);

        // transient!! there must be no record
        try {
            Assert.assertEquals(0,
                    aliceSimplePeer.getASAPStorage(APPNAME).getChannel(TestConstants.URI).getMessages().size());
        }
        catch(ASAPException e) {
            // ok - no trace
        }
        try {
            Assert.assertEquals(0,
                    bobSimplePeer.getASAPStorage(APPNAME).getChannel(TestConstants.URI).getMessages().size());
        }
            catch(ASAPException e) {
            // ok - no trace
        }

        try {
            ASAPInternalChunk chunk = aliceSimplePeer.getASAPStorage(APPNAME).getIncomingStorage(TestConstants.BOB_ID)
                    .getChunkStorage().getChunk(TestConstants.URI, ASAP.TRANSIENT_ERA);

            Assert.assertFalse(chunk.getMessages().hasNext());
        }
        catch(ASAPException e) {
            // ok there must be no trace
        }

        try {
            ASAPInternalChunk chunk = bobSimplePeer.getASAPStorage(APPNAME).getIncomingStorage(TestConstants.BOB_ID)
                    .getChunkStorage().getChunk(TestConstants.URI, ASAP.TRANSIENT_ERA);

            Assert.assertFalse(chunk.getMessages().hasNext());
        }
        catch(ASAPException e) {
            // ok there must be no trace
        }

    }
}
