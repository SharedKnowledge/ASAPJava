package junit5Tests.release_1;

import net.sharksystem.TestConstants;
import net.sharksystem.TestHelper;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.apps.testsupport.ASAPTestPeerFS;
import net.sharksystem.asap.engine.ASAPEngineFS;
import net.sharksystem.asap.mockAndTemplates.ASAPMessageReceivedListenerExample;
import net.sharksystem.asap.mockAndTemplates.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Rankhole, thsc42
 */
public class MultipleEncounterTests {
    private static final String TEST_FOLDER = "MultipleEncounterTests";
    public static final String ANIMAL = "animal";
    public static final String WATER = "water";
    public static final String ICE_CREAM = "ice_cream";

    public static final String TIGER_URI = "tiger";
    public static final String ELEPHANT_URI = "elephant";
    public static final String HELLO_URI = "hello";

    public static final String ALICE_URI_1 = "aliceUri1";
    public static final String ALICE_URI_2 = "aliceUri2";
    public static final String ALICE_URI_3 = "aliceUri3";
    public static final String ALICE_URI_4 = "aliceUri4";

    public static final String BOB_URI_1 = "bobUri1";
    public static final String BOB_URI_2 = "bobUri2";
    public static final String BOB_URI_3 = "bobUri3";
    public static final String BOB_URI_4 = "bobUri4";

    private static final String EXAMPLE_APP_FORMAT = "exampleAppFormat";

    private Collection<CharSequence> formats;
    private static final int PORT = 7777;
    private static int port = 0;

    /* clear folder before launching tests */
    public MultipleEncounterTests() {
        ASAPEngineFS.removeFolder(TEST_FOLDER);
    }

    //ASAPPeerFSTestHelper testHelper;
    private ASAPTestPeerFS aliceTestPeer, bobTestPeer;

    @BeforeEach
    public void setUp() throws IOException, ASAPException {
        // delete directory „testPeerFS” to prevent errors when running twice
        //FileUtils.deleteDirectory(new File("testPeerFS"));

        // root folder of all ASAPPeers
        //String rootfolder = "./testPeerFS";

        // create a new folder for each test - avoids conflicts when removal does not work due to race condition
        String rootfolder = TestHelper.getFullTempFolderName(TEST_FOLDER, true);

        formats = new ArrayList<>();
        formats.add(EXAMPLE_APP_FORMAT);

        // helper class with useful testing functions
        // testHelper = new ASAPPeerFSTestHelper(rootfolder, EXAMPLE_APP_FORMAT);

        this.aliceTestPeer = new ASAPTestPeerFS(
                TestConstants.ALICE_ID, rootfolder + "/" + TestConstants.ALICE_NAME, formats);
        this.bobTestPeer = new ASAPTestPeerFS(
                TestConstants.BOB_ID, rootfolder + "/" + TestConstants.BOB_NAME, formats);
    }

    @Test
    public void singleEncounter_differentURIs() throws InterruptedException, IOException, ASAPException {
        String uriAlice = ANIMAL;
        String uriBob = WATER;

        simpleEncounterWithMessageExchange(uriAlice, uriBob);

        // chunks should be created for each send message
        // this will only be checked on single encounter tests, as in further tests this gets more and more tedious to do
        Assertions.assertTrue(aliceTestPeer.getASAPStorage(EXAMPLE_APP_FORMAT).getChunkStorage().existsChunk(uriAlice, 0));
        Assertions.assertTrue(bobTestPeer.getASAPStorage(EXAMPLE_APP_FORMAT).getChunkStorage().existsChunk(uriBob, 0));
        Assertions.assertTrue(aliceTestPeer.getASAPStorage(EXAMPLE_APP_FORMAT).getChunkStorage().existsChunk(uriBob, 1));
        Assertions.assertTrue(bobTestPeer.getASAPStorage(EXAMPLE_APP_FORMAT).getChunkStorage().existsChunk(uriAlice, 1));

        // each message should have created a new era, so there should be a meta and content file in each subfolder
        Assertions.assertTrue(
                this.senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, uriBob, 0));
        Assertions.assertTrue(
                this.senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, uriAlice, 0));
/*
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.ALICE_ID, TestConstants.BOB_ID, uriBob, 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.BOB_ID, TestConstants.ALICE_ID, uriAlice, 0));
 */
    }

    @Test
    public void singleEncounter_sameURIs() throws InterruptedException, IOException, ASAPException {
        String uriAlice = ANIMAL;
        String uriBob = ANIMAL;

        simpleEncounterWithMessageExchange(uriAlice, uriBob);

        // chunks should be created for each send message
        // this will only be checked on single encounter tests, as in further tests this gets more and more tedious to do
        Assertions.assertTrue(aliceTestPeer.getASAPStorage(EXAMPLE_APP_FORMAT).getChunkStorage().existsChunk(uriAlice, 0));
        Assertions.assertTrue(bobTestPeer.getASAPStorage(EXAMPLE_APP_FORMAT).getChunkStorage().existsChunk(uriBob, 0));
        Assertions.assertTrue(aliceTestPeer.getASAPStorage(EXAMPLE_APP_FORMAT).getChunkStorage().existsChunk(uriBob, 1));
        Assertions.assertTrue(bobTestPeer.getASAPStorage(EXAMPLE_APP_FORMAT).getChunkStorage().existsChunk(uriAlice, 1));


        // each message should have created a new era, so there should be a meta and content file in each subfolder
        Assertions.assertTrue(
                this.senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, uriBob, 0));
/*
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.ALICE_ID, TestConstants.BOB_ID, uriBob, 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.BOB_ID, TestConstants.ALICE_ID, uriAlice, 0));
 */
    }

    @Test
    public void multiEncounter_sameURIs() throws InterruptedException, IOException, ASAPException {
        String uri = ICE_CREAM;
        int numberOfLoops = 4;
        for(int i = 0; i < numberOfLoops; i++) {
            simpleEncounterWithMessageExchange(uri, uri);
        }

        for(int era = 0; era < numberOfLoops; era++) {
            Assertions.assertTrue(
                    senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, uri, era));

            Assertions.assertTrue(
                    senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, uri, era));
        }

/*
        // each message should have created a new era, so there should be a meta and content file in each subfolder
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.ALICE_ID, TestConstants.BOB_ID, uri, 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.ALICE_ID, TestConstants.BOB_ID, uri, 1));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.ALICE_ID, TestConstants.BOB_ID, uri, 2));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.ALICE_ID, TestConstants.BOB_ID, uri, 3));

        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.BOB_ID, TestConstants.ALICE_ID, uri, 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.BOB_ID, TestConstants.ALICE_ID, uri, 1));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.BOB_ID, TestConstants.ALICE_ID, uri, 2));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.BOB_ID, TestConstants.ALICE_ID, uri, 3));
 */
    }

    @Test
    public void multiEncounter_partiallyDifferentURIs() throws InterruptedException, IOException, ASAPException {
        // sending different uris back and forth now breaks it
        simpleEncounterWithMessageExchange(TIGER_URI, ICE_CREAM, 1);
        simpleEncounterWithMessageExchange(ICE_CREAM, ELEPHANT_URI, 2);
        simpleEncounterWithMessageExchange(HELLO_URI, ICE_CREAM, 3);
        simpleEncounterWithMessageExchange(ICE_CREAM, HELLO_URI, 4);

        int era = 0;
        // encounter #1: Alice (TIGER_URI) < - > Bob (ICE_CREAM)
        Assertions.assertTrue(
                senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, ICE_CREAM, era));
        Assertions.assertTrue(
                senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, TIGER_URI, era));

        era++;
        // encounter #2: Alice (ICE_CREAM) < - > Bob (ELEPHANT_URI)
        Assertions.assertTrue(
                senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, ELEPHANT_URI, era));
        Assertions.assertTrue(
                senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, ICE_CREAM, era));

        era++;
        // encounter #3: Alice (HELLO_URI) < - > Bob (ICE_CREAM)
        Assertions.assertTrue(
                senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, ICE_CREAM, era));
        Assertions.assertTrue(
                senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, HELLO_URI, era));

        era++;
        // encounter #4: Alice (ICE_CREAM) < - > Bob (HELLO_URI)
        Assertions.assertTrue(
                senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, HELLO_URI, era));
        Assertions.assertTrue(
                senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, ICE_CREAM, era));

/*
        // all eras SHOULD exists, but some are missing!
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.ALICE_ID, TestConstants.BOB_ID, ICE_CREAM, 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.ALICE_ID, TestConstants.BOB_ID, ELEPHANT_URI, 1));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.ALICE_ID, TestConstants.BOB_ID, ICE_CREAM, 2));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.ALICE_ID, TestConstants.BOB_ID, HELLO_URI, 3));

        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.BOB_ID, TestConstants.ALICE_ID, TIGER_URI, 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.BOB_ID, TestConstants.ALICE_ID, ICE_CREAM, 1));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.BOB_ID, TestConstants.ALICE_ID, HELLO_URI, 2));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.BOB_ID, TestConstants.ALICE_ID, ICE_CREAM, 3));
 */
    }

    @Test
    public void multiEncounter_completelyDifferentURIs() throws InterruptedException, IOException, ASAPException {

        int numberEncounter = 4;
        String[][] exchangedUris = new String[numberEncounter][];

        // define what to exchange in each encounter
        exchangedUris[0] = new String[] {ALICE_URI_1, BOB_URI_1};
        exchangedUris[1] = new String[] {ALICE_URI_2, BOB_URI_2};
        exchangedUris[2] = new String[] {ALICE_URI_3, BOB_URI_3};
        exchangedUris[3] = new String[] {ALICE_URI_4, BOB_URI_4};

        // run encounter
        for(int i = 0; i < numberEncounter; i++) {
            simpleEncounterWithMessageExchange(exchangedUris[i][0], exchangedUris[i][1], i);
        }

        // test
        for(int era = 0; era < numberEncounter; era++) {
            System.out.print("check era #" + era);
            // Alice got Bob's uri
            Assertions.assertTrue(
                    senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT,
                            TestConstants.BOB_ID, exchangedUris[era][1], era));
            // Bob received Alice's uri
            Assertions.assertTrue(
                    senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT,
                            TestConstants.ALICE_ID, exchangedUris[era][0], era));
        }

        /*
        // we expect ALL of the uris to exist, but they don't.
        // only two messages of bob and one message of alice gets transmitted, then it stops transmitting for the rest..
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.ALICE_ID, TestConstants.BOB_ID, BOB_URI_1, 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.ALICE_ID, TestConstants.BOB_ID, BOB_URI_2, 1));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.ALICE_ID, TestConstants.BOB_ID, BOB_URI_3, 2));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.ALICE_ID, TestConstants.BOB_ID, BOB_URI_4, 3));

        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.BOB_ID, TestConstants.ALICE_ID, ALICE_URI_1, 0));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.BOB_ID, TestConstants.ALICE_ID, ALICE_URI_2, 1));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.BOB_ID, TestConstants.ALICE_ID, ALICE_URI_3, 2));
        Assertions.assertTrue(testHelper.senderEraShouldExist(TestConstants.BOB_ID, TestConstants.ALICE_ID, ALICE_URI_4, 3));
         */
    }

    public void simpleEncounterWithMessageExchange(String uriAlice, String uriBob)
            throws IOException, ASAPException, InterruptedException {
        this.simpleEncounterWithMessageExchange(uriAlice, uriBob, 0);
    }


    // sends messages with given uri, starts and then stops the encounter
    // message content is irrelevant, we don't test for it
    public void simpleEncounterWithMessageExchange(String uriAlice, String uriBob, int encounterNumber)
            throws IOException, ASAPException, InterruptedException {

        // simulate ASAP first encounter with full ASAP protocol stack and engines
        System.out.println("+++++++++++++++++++ encounter #" + encounterNumber + " starts soon ++++++++++++++++++++");
        Thread.sleep(50);

        // setup message received listener - this should be replaced with your code - you implement a listener.
        ASAPMessageReceivedListenerExample aliceMessageReceivedListenerExample =
                new ASAPMessageReceivedListenerExample();

        aliceTestPeer.addASAPMessageReceivedListener(EXAMPLE_APP_FORMAT, aliceMessageReceivedListenerExample);

        // example - this should be produced by your application
        byte[] serializedData = TestUtils.serializeExample(42, "from alice", true);

        aliceTestPeer.sendASAPMessage(EXAMPLE_APP_FORMAT, uriAlice, serializedData);

        ///////////////// BOB //////////////////////////////////////////////////////////////

        // this should be replaced with your code - you must implement a listener.
        ASAPMessageReceivedListenerExample asapMessageReceivedListenerExample =
                new ASAPMessageReceivedListenerExample();

        // register your listener (or that mock) with asap connection mock
        bobTestPeer.addASAPMessageReceivedListener(EXAMPLE_APP_FORMAT, asapMessageReceivedListenerExample);

        // bob writes something
        bobTestPeer.sendASAPMessage(EXAMPLE_APP_FORMAT, uriBob,
                TestUtils.serializeExample(43, "from bob", false));
        bobTestPeer.sendASAPMessage(EXAMPLE_APP_FORMAT, uriBob,
                TestUtils.serializeExample(44, "from bob again", false));

        // give your app a moment to process
        Thread.sleep(500);
        // start actual encounter
        aliceTestPeer.startEncounter(TestHelper.getPortNumber(), bobTestPeer);

        // give your app a moment to process
        Thread.sleep(1000);
        // stop encounter
        bobTestPeer.stopEncounter(aliceTestPeer);
        // give your app a moment to process
        Thread.sleep(1000);
    }

    public boolean senderEraShouldExist(ASAPPeer peer, String format, String sender, String uri, int era)
            throws IOException, ASAPException {
        return peer.getASAPStorage(format).getExistingIncomingStorage(sender).getChunkStorage().existsChunk(uri, era);
    }
}
