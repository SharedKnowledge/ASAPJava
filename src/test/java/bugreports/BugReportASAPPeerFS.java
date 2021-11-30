package bugreports;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeerFS;
import net.sharksystem.asap.apps.testsupport.ASAPTestPeerFS;
import net.sharksystem.asap.mockAndTemplates.ASAPMessageReceivedListenerExample;
import net.sharksystem.asap.mockAndTemplates.TestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class BugReportASAPPeerFS {

    ASAPPeerFSTestHelper testHelper;

    private ASAPTestPeerFS aliceTestPeer, bobTestPeer;

    private String TEST_URI = "sn://test";

    private Collection<CharSequence> formats;

    private CharSequence format = "ASAPTest";

    private static final int PORT = 7777;

    private static int port = 0;

    static int getPortNumber() {
        if (BugReportASAPPeerFS.port == 0) {
            BugReportASAPPeerFS.port = PORT;
        } else {
            BugReportASAPPeerFS.port++;
        }

        return BugReportASAPPeerFS.port;
    }


    @BeforeEach
    public void setUp() throws IOException, ASAPException {
        // delete directory „testPeerFS” to prevent errors when running twice
        FileUtils.deleteDirectory(new File("testPeerFS"));

        // root folder of all ASAPPeers
        String rootfolder = "./testPeerFS";

        formats = new ArrayList<>();
        formats.add(format);

        // helper class with usefull testing functions
        testHelper = new ASAPPeerFSTestHelper(rootfolder, format);

        this.aliceTestPeer = new ASAPTestPeerFS("ALICE", rootfolder + "/ALICE", formats);
        this.bobTestPeer = new ASAPTestPeerFS("BOB", rootfolder + "/BOB", formats);
    }

    @Test
    public void singleEncounter_differentURIs() throws InterruptedException, IOException, ASAPException {
        String uriAlice = "Tier";
        String uriBob = "Wasser";

        simpleEncounterWithMessageExchange(uriAlice, uriBob);

        // chunks should be created for each send message
        // this will only be checked on single encounter tests, as in further tests this gets more and more tedious to do
        Assertions.assertTrue(aliceTestPeer.getASAPStorage(format).getChunkStorage().existsChunk(uriAlice, 0));
        Assertions.assertTrue(bobTestPeer.getASAPStorage(format).getChunkStorage().existsChunk(uriBob, 0));
        Assertions.assertTrue(aliceTestPeer.getASAPStorage(format).getChunkStorage().existsChunk(uriBob, 1));
        Assertions.assertTrue(bobTestPeer.getASAPStorage(format).getChunkStorage().existsChunk(uriAlice, 1));

        // each message should have created a new era, so there should be a meta and content file in each subfolder
        Assertions.assertTrue(testHelper.senderEraShouldExist("ALICE", "BOB", uriBob, 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist("BOB", "ALICE", uriAlice, 0));
    }

    @Test
    public void singleEncounter_sameURIs() throws InterruptedException, IOException, ASAPException {
        String uriAlice = "Tier";
        String uriBob = "Tier";

        simpleEncounterWithMessageExchange(uriAlice, uriBob);

        // chunks should be created for each send message
        // this will only be checked on single encounter tests, as in further tests this gets more and more tedious to do
        Assertions.assertTrue(aliceTestPeer.getASAPStorage(format).getChunkStorage().existsChunk(uriAlice, 0));
        Assertions.assertTrue(bobTestPeer.getASAPStorage(format).getChunkStorage().existsChunk(uriBob, 0));
        Assertions.assertTrue(aliceTestPeer.getASAPStorage(format).getChunkStorage().existsChunk(uriBob, 1));
        Assertions.assertTrue(bobTestPeer.getASAPStorage(format).getChunkStorage().existsChunk(uriAlice, 1));

        // each message should have created a new era, so there should be a meta and content file in each subfolder
        Assertions.assertTrue(testHelper.senderEraShouldExist("ALICE", "BOB", uriBob, 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist("BOB", "ALICE", uriAlice, 0));
    }

    @Test
    public void multiEncounter_sameURIs() throws InterruptedException, IOException, ASAPException {

        String uri = "Eis";

        simpleEncounterWithMessageExchange(uri, uri);

        simpleEncounterWithMessageExchange(uri, uri);

        simpleEncounterWithMessageExchange(uri, uri);

        simpleEncounterWithMessageExchange(uri, uri);

        // each message should have created a new era, so there should be a meta and content file in each subfolder
        Assertions.assertTrue(testHelper.senderEraShouldExist("ALICE", "BOB", uri, 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist("ALICE", "BOB", uri, 1));
        Assertions.assertTrue(testHelper.senderEraShouldExist("ALICE", "BOB", uri, 2));
        Assertions.assertTrue(testHelper.senderEraShouldExist("ALICE", "BOB", uri, 3));

        Assertions.assertTrue(testHelper.senderEraShouldExist("BOB", "ALICE", uri, 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist("BOB", "ALICE", uri, 1));
        Assertions.assertTrue(testHelper.senderEraShouldExist("BOB", "ALICE", uri, 2));
        Assertions.assertTrue(testHelper.senderEraShouldExist("BOB", "ALICE", uri, 3));
    }

    @Test
    public void multiEncounter_partiallyDifferentURIs() throws InterruptedException, IOException, ASAPException {

        String uri = "Eis";

        // sending different uris back and forth now breaks it
        simpleEncounterWithMessageExchange("Tiger", uri);

        simpleEncounterWithMessageExchange(uri, "Elefant");

        simpleEncounterWithMessageExchange("Hallo", uri);

        simpleEncounterWithMessageExchange(uri, "Hallo");

        // all eras SHOULD exists, but some are missing!
        Assertions.assertTrue(testHelper.senderEraShouldExist("ALICE", "BOB", uri, 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist("ALICE", "BOB", "Elefant", 1));
        Assertions.assertTrue(testHelper.senderEraShouldExist("ALICE", "BOB", uri, 2));
        Assertions.assertTrue(testHelper.senderEraShouldExist("ALICE", "BOB", "Hallo", 3));

        Assertions.assertTrue(testHelper.senderEraShouldExist("BOB", "ALICE", "Tiger", 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist("BOB", "ALICE", uri, 1));
        Assertions.assertTrue(testHelper.senderEraShouldExist("BOB", "ALICE", "Hallo", 2));
        Assertions.assertTrue(testHelper.senderEraShouldExist("BOB", "ALICE", uri, 3));
    }

    @Test
    public void multiEncounter_completelyDifferentURIs() throws InterruptedException, IOException, ASAPException {

        String uriAlice = "aliceUri1";
        String uriBob = "bobUri1";

        simpleEncounterWithMessageExchange(uriAlice, uriBob);

        uriAlice = "aliceUri2";
        uriBob = "bobUri2";
        simpleEncounterWithMessageExchange(uriAlice, uriBob);

        uriAlice = "aliceUri3";
        uriBob = "bobUri3";
        simpleEncounterWithMessageExchange(uriAlice, uriBob);

        uriAlice = "aliceUri4";
        uriBob = "bobUri4";
        simpleEncounterWithMessageExchange(uriAlice, uriBob);

        // we expect ALL of the uris to exist, but they don't.
        // only two messages of bob and one message of alice gets transmitted, then it stops transmitting for the rest..
        Assertions.assertTrue(testHelper.senderEraShouldExist("ALICE", "BOB", "bobUri1", 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist("ALICE", "BOB", "bobUri2", 1));
        Assertions.assertTrue(testHelper.senderEraShouldExist("ALICE", "BOB", "bobUri3", 2));
        Assertions.assertTrue(testHelper.senderEraShouldExist("ALICE", "BOB", "bobUri4", 3));

        Assertions.assertTrue(testHelper.senderEraShouldExist("BOB", "ALICE", "aliceUri1", 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist("BOB", "ALICE", "aliceUri2", 1));
        Assertions.assertTrue(testHelper.senderEraShouldExist("BOB", "ALICE", "aliceUri3", 2));
        Assertions.assertTrue(testHelper.senderEraShouldExist("BOB", "ALICE", "aliceUri4", 3));
    }

    // sends messages with given uri, starts and then stops the encounter
    // message content is irrelevant, we don't test for it
    public void simpleEncounterWithMessageExchange(String uriAlice, String uriBob)
            throws IOException, ASAPException, InterruptedException {

        // simulate ASAP first encounter with full ASAP protocol stack and engines
        System.out.println("+++++++++++++++++++ 1st encounter starts soon ++++++++++++++++++++");
        Thread.sleep(50);

        // setup message received listener - this should be replaced with your code - you implement a listener.
        ASAPMessageReceivedListenerExample aliceMessageReceivedListenerExample =
                new ASAPMessageReceivedListenerExample();

        aliceTestPeer.addASAPMessageReceivedListener(format, aliceMessageReceivedListenerExample);

        // example - this should be produced by your application
        byte[] serializedData = TestUtils.serializeExample(42, "from alice", true);

        aliceTestPeer.sendASAPMessage(format, uriAlice, serializedData);

        ///////////////// BOB //////////////////////////////////////////////////////////////

        // this should be replaced with your code - you must implement a listener.
        ASAPMessageReceivedListenerExample asapMessageReceivedListenerExample =
                new ASAPMessageReceivedListenerExample();

        // register your listener (or that mock) with asap connection mock
        bobTestPeer.addASAPMessageReceivedListener(format, asapMessageReceivedListenerExample);

        // bob writes something
        bobTestPeer.sendASAPMessage(format, uriBob,
                TestUtils.serializeExample(43, "from bob", false));
        bobTestPeer.sendASAPMessage(format, uriBob,
                TestUtils.serializeExample(44, "from bob again", false));

        // give your app a moment to process
        Thread.sleep(500);
        // start actual encounter
        aliceTestPeer.startEncounter(getPortNumber(), bobTestPeer);

        // give your app a moment to process
        Thread.sleep(1000);
        // stop encounter
        bobTestPeer.stopEncounter(aliceTestPeer);
        // give your app a moment to process
        Thread.sleep(1000);
    }

}
