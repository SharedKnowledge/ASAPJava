package junit5Tests.release_1.net.sharksystem.asap;

import net.sharksystem.SharkException;
import net.sharksystem.asap.*;
import net.sharksystem.asap.apps.TCPServerSocketAcceptor;
import net.sharksystem.asap.apps.testsupport.ASAPTestPeerFS;
import net.sharksystem.testhelper.ASAPTesthelper;
import net.sharksystem.utils.streams.StreamPair;
import net.sharksystem.utils.streams.StreamPairImpl;
import net.sharksystem.utils.testsupport.ASAPMessageReceivedStorage;
import net.sharksystem.utils.testsupport.TestConstants;
import net.sharksystem.utils.testsupport.TestHelper;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class LSANEncounterManagerAdminTests {
    public static final String TEST_CLASS_ROOTFOLDER = TestConstants.ROOT_DIRECTORY + "encounterAdmin/";
    public static final String ALICE_ROOTFOLDER = TEST_CLASS_ROOTFOLDER + TestConstants.ALICE_NAME;
    public static final String BOB_ROOTFOLDER = TEST_CLASS_ROOTFOLDER + TestConstants.BOB_NAME;

    public Set<CharSequence> supportedFormats;
    private ASAPEncounterManagerImpl aliceEncounterManager, bobEncounterManager;
    private ASAPPeerFS aliceASAPPeerFS, bobASAPPeerFS;

    ASAPMessageReceivedStorage aliceASAPMessageReceivedListener, bobASAPMessageReceivedListener;

    @BeforeEach
    public void setUp() throws IOException, SharkException {
        this.supportedFormats = new HashSet<>();
        this.supportedFormats.add(TestConstants.TEST_APP_FORMAT);

        String aliceFolder = ASAPTesthelper.getUniqueFolderName(ALICE_ROOTFOLDER.toString());
        String bobFolder = ASAPTesthelper.getUniqueFolderName(BOB_ROOTFOLDER.toString());
        TestHelper.incrementTestNumber();

        //////////////////////////////// setup Alice
        TestHelper.removeFolder(aliceFolder); // clean folder from previous tests
        TestHelper.removeFolder(bobFolder); // clean folder from previous tests

        aliceASAPPeerFS = new ASAPTestPeerFS(TestConstants.ALICE_ID, aliceFolder, this.supportedFormats);
        aliceASAPMessageReceivedListener = new ASAPMessageReceivedStorage();
        aliceASAPPeerFS.addASAPMessageReceivedListener(TestConstants.TEST_APP_FORMAT, aliceASAPMessageReceivedListener);

        bobASAPPeerFS = new ASAPTestPeerFS(TestConstants.BOB_ID, bobFolder, this.supportedFormats);
        bobASAPMessageReceivedListener = new ASAPMessageReceivedStorage();
        bobASAPPeerFS.addASAPMessageReceivedListener(TestConstants.TEST_APP_FORMAT, bobASAPMessageReceivedListener);

        aliceEncounterManager = new ASAPEncounterManagerImpl(aliceASAPPeerFS, TestConstants.ALICE_ID);
        bobEncounterManager = new ASAPEncounterManagerImpl(bobASAPPeerFS, TestConstants.BOB_ID);
    }

    private void runAliceBobEncounter() throws IOException, InterruptedException {
        int port = TestHelper.getPortNumber();
        System.out.println(">>>>>>>>>>>>> START RUNNING ENCOUNTER ALICE <-> BOB (initiator) on port " + port);
        // open port on alice side - and run it through her encounter manager
        new TCPServerSocketAcceptor(port, aliceEncounterManager);
        // connect from bob side
        Socket connect2Alice = new Socket("localhost", port);
        StreamPair bob2AliceStreamPair = StreamPairImpl.getStreamPair(connect2Alice.getInputStream(), connect2Alice.getOutputStream(),
                null, TestConstants.ALICE_ID);

        // Alice will handle
        bobEncounterManager.handleEncounter(bob2AliceStreamPair, ASAPEncounterConnectionType.INTERNET);
        Thread.sleep(200);
    }

    @Test
    public void testOneMessageA2B() throws InterruptedException, IOException, ASAPException {
        aliceASAPPeerFS.sendASAPMessage(TestConstants.TEST_APP_FORMAT, TestConstants.URI, TestConstants.MESSAGE_1);
        this.runAliceBobEncounter();
        Assert.assertEquals(1, bobASAPMessageReceivedListener.getNumberReceivedMessages());

    }

    @Test
    public void testOneMessageA2BFailsBecauseBothBlocked() throws InterruptedException, IOException, ASAPException {
        int port = TestHelper.getPortNumber();
        aliceASAPPeerFS.sendASAPMessage(TestConstants.TEST_APP_FORMAT, TestConstants.URI, TestConstants.MESSAGE_1);
        aliceEncounterManager.addToDenyList(TestConstants.BOB_ID);
        bobEncounterManager.addToDenyList(TestConstants.ALICE_ID);

        this.runAliceBobEncounter();

        Assert.assertEquals(0, bobASAPMessageReceivedListener.getNumberReceivedMessages());
    }

    @Test
    public void testOneMessageA2BFailsBecauseOneBlocked() throws InterruptedException, IOException, ASAPException {
        int port = TestHelper.getPortNumber();
        aliceASAPPeerFS.sendASAPMessage(TestConstants.TEST_APP_FORMAT, TestConstants.URI, TestConstants.MESSAGE_1);
        aliceEncounterManager.addToDenyList(TestConstants.BOB_ID);

        this.runAliceBobEncounter();

        Assert.assertEquals(0, bobASAPMessageReceivedListener.getNumberReceivedMessages());
    }

    @Test
    public void testAliceSendsTwoMessages() throws InterruptedException, IOException, ASAPException {
        // send message when offline
        aliceASAPPeerFS.sendASAPMessage(TestConstants.TEST_APP_FORMAT, TestConstants.URI, TestConstants.MESSAGE_1);
        this.runAliceBobEncounter();
        // send message when online
        aliceASAPPeerFS.sendASAPMessage(TestConstants.TEST_APP_FORMAT, TestConstants.URI, TestConstants.MESSAGE_2);
        Thread.sleep(200);
        Assert.assertEquals(2, bobASAPMessageReceivedListener.getNumberReceivedMessages());

    }

    @Test
    public void testAliceSendsOneMessageConnectionKilled() throws InterruptedException, IOException, ASAPException {
        // send message when offline
        aliceASAPPeerFS.sendASAPMessage(TestConstants.TEST_APP_FORMAT, TestConstants.URI, TestConstants.MESSAGE_1);
        this.runAliceBobEncounter();

        // kill connection via encounter manager
        aliceEncounterManager.closeEncounter(TestConstants.BOB_ID);
        // try to send another message
        aliceASAPPeerFS.sendASAPMessage(TestConstants.TEST_APP_FORMAT, TestConstants.URI, TestConstants.MESSAGE_2);
        Thread.sleep(200);
        // second message should not reach Bob - connection closed. It would get delivered after a reconnect
        Assert.assertEquals(1, bobASAPMessageReceivedListener.getNumberReceivedMessages());
    }

    @Test
    public void testPersistDenyList() throws SharkException, IOException, InterruptedException {
        String emPeristFolder = TEST_CLASS_ROOTFOLDER + "persistEncManagerTest";

        ASAPEncounterManagerImpl encounterManager =
                new ASAPEncounterManagerImpl(null, null,1, emPeristFolder);

        encounterManager.clearDenyList();
        encounterManager.addToDenyList(TestConstants.ALICE_ID);

        // restore
        encounterManager = new ASAPEncounterManagerImpl(null, null,1, emPeristFolder);

        Set<CharSequence> denyList = encounterManager.getDenyList();
        Assert.assertEquals(1, denyList.size());
        Assert.assertTrue(denyList.contains(TestConstants.ALICE_ID));

        // clear
        encounterManager.clearDenyList();
//        Thread.sleep(100);
        // reload again
        encounterManager = new ASAPEncounterManagerImpl(null, null,1, emPeristFolder);
        denyList = encounterManager.getDenyList();
        Assert.assertEquals(0, denyList.size());
    }
}