package junit5Tests.release_1;

import net.sharksystem.asap.apps.testsupport.TestConstants;
import net.sharksystem.asap.apps.testsupport.TestHelper;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.apps.testsupport.ASAPTestPeerFS;
import net.sharksystem.asap.engine.ASAPEngineFS;
import net.sharksystem.asap.mockAndTemplates.ASAPMessageReceivedListenerExample;
import net.sharksystem.asap.mockAndTemplates.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
    public static final String ANIMAL_URI = "animal";
    public static final String WATER_URI = "water";
    public static final String ICE_CREAM_URI = "ice_cream";

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

    @BeforeAll
    public static void removePreviousTestFolder() {
        ASAPEngineFS.removeFolder(TestConstants.ROOT_DIRECTORY + TEST_FOLDER);
    }

    private ASAPTestPeerFS aliceTestPeer, bobTestPeer, claraTestPeer;

    @BeforeEach
    public void setUp() throws IOException, ASAPException {
        // create a new folder for each test - avoids conflicts when removal does not work due to race condition
        String rootfolder = TestHelper.getFullTempFolderName(TEST_FOLDER, true);

        formats = new ArrayList<>();
        formats.add(EXAMPLE_APP_FORMAT);

        this.aliceTestPeer = new ASAPTestPeerFS(
                TestConstants.ALICE_ID, rootfolder + "/" + TestConstants.ALICE_NAME, formats);
        this.bobTestPeer = new ASAPTestPeerFS(
                TestConstants.BOB_ID, rootfolder + "/" + TestConstants.BOB_NAME, formats);
        this.claraTestPeer = new ASAPTestPeerFS(
                TestConstants.CLARA_ID, rootfolder + "/" + TestConstants.CLARA_NAME, formats);
    }

    @Test
    public void singleEncounter_differentURIs() throws InterruptedException, IOException, ASAPException {
        String uriAlice = ANIMAL_URI;
        String uriBob = WATER_URI;

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
    }

    @Test
    public void singleEncounter_sameURIs() throws InterruptedException, IOException, ASAPException {
        String uriAlice = ANIMAL_URI;
        String uriBob = ANIMAL_URI;

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
    }

    @Test
    public void multiEncounter_sameURIs() throws InterruptedException, IOException, ASAPException {
        String uri = ICE_CREAM_URI;
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
    }

    @Test
    public void multiEncounter_partiallyDifferentURIs() throws InterruptedException, IOException, ASAPException {
        // sending different uris back and forth now breaks it
        simpleEncounterWithMessageExchange(TIGER_URI, ICE_CREAM_URI, 1);
        simpleEncounterWithMessageExchange(ICE_CREAM_URI, ELEPHANT_URI, 2);
        simpleEncounterWithMessageExchange(HELLO_URI, ICE_CREAM_URI, 3);
        simpleEncounterWithMessageExchange(ICE_CREAM_URI, HELLO_URI, 4);

        int era = 0;
        // encounter #1: Alice (TIGER_URI) < - > Bob (ICE_CREAM)
        Assertions.assertTrue(
                senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, ICE_CREAM_URI, era));
        Assertions.assertTrue(
                senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, TIGER_URI, era));

        era++;
        // encounter #2: Alice (ICE_CREAM) < - > Bob (ELEPHANT_URI)
        Assertions.assertTrue(
                senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, ELEPHANT_URI, era));
        Assertions.assertTrue(
                senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, ICE_CREAM_URI, era));

        era++;
        // encounter #3: Alice (HELLO_URI) < - > Bob (ICE_CREAM)
        Assertions.assertTrue(
                senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, ICE_CREAM_URI, era));
        Assertions.assertTrue(
                senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, HELLO_URI, era));

        era++;
        // encounter #4: Alice (ICE_CREAM) < - > Bob (HELLO_URI)
        Assertions.assertTrue(
                senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, HELLO_URI, era));
        Assertions.assertTrue(
                senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, ICE_CREAM_URI, era));
    }

    @Test
    public void multiEncounter_completelyDifferentURIs() throws InterruptedException, IOException, ASAPException {

        int numberEncounter = 4;
        String[][] exchangedUris = new String[numberEncounter][];

        int aliceIndex = 0; // take index 0 for alice uri
        int bobIndex = 1; // take index 0 for bob uri

        // define what to exchange in each encounter
        exchangedUris[0] = new String[] {ALICE_URI_1, BOB_URI_1}; // alice at index 0 && bob at index 1
        exchangedUris[1] = new String[] {ALICE_URI_2, BOB_URI_2};
        exchangedUris[2] = new String[] {ALICE_URI_3, BOB_URI_3};
        exchangedUris[3] = new String[] {ALICE_URI_4, BOB_URI_4};

        // run encounter
        for(int i = 0; i < numberEncounter; i++) {
            simpleEncounterWithMessageExchange(exchangedUris[i][aliceIndex], exchangedUris[i][bobIndex], i);
        }

        // test
        for(int era = 0; era < numberEncounter; era++) {
            System.out.print("check era #" + era);
            // Alice got Bob's uri
            Assertions.assertTrue(
                    senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT,
                            TestConstants.BOB_ID, exchangedUris[era][bobIndex], era));
            // Bob received Alice's uri
            Assertions.assertTrue(
                    senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT,
                            TestConstants.ALICE_ID, exchangedUris[era][aliceIndex], era));
        }
    }
    
    @Test
    public void multipleEncounterRouting() throws IOException, ASAPException, InterruptedException {
        String alice2claraURI = "HelloToClara";
        String clara2aliceURI = "FromClara";
        int enounterCounter = 0;
        simpleEncounterWithMessageExchange(TIGER_URI, ANIMAL_URI, enounterCounter++);
        simpleEncounterWithMessageExchange(ANIMAL_URI, ELEPHANT_URI, enounterCounter++);
        simpleEncounterWithMessageExchange(HELLO_URI, ANIMAL_URI, enounterCounter++);
        simpleEncounterWithMessageExchange(ANIMAL_URI, HELLO_URI, enounterCounter++);

        // as always, alice and bob should have received all four messages
        Assertions.assertTrue(senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, ANIMAL_URI, 0));
        Assertions.assertTrue(senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, ELEPHANT_URI, 1));
        Assertions.assertTrue(senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, ANIMAL_URI, 2));
        Assertions.assertTrue(senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, HELLO_URI, 3));

        Assertions.assertTrue(senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, TIGER_URI, 0));
        Assertions.assertTrue(senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, ANIMAL_URI, 1));
        Assertions.assertTrue(senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, HELLO_URI, 2));
        Assertions.assertTrue(senderEraShouldExist(bobTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, ANIMAL_URI, 3));

        // check if routing is allowed (should be by default)
        Assertions.assertTrue(aliceTestPeer.isASAPRoutingAllowed(EXAMPLE_APP_FORMAT));
        // exchange between alice and clara
        simpleEncounterWithMessageExchange(aliceTestPeer, claraTestPeer, alice2claraURI, clara2aliceURI, enounterCounter++);
        // Alice should have received message from Clara
        Assertions.assertTrue(senderEraShouldExist(aliceTestPeer, EXAMPLE_APP_FORMAT, TestConstants.CLARA_ID, clara2aliceURI, 0));
        // and vice versa
        Assertions.assertTrue(senderEraShouldExist(claraTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, alice2claraURI, 4));

        // all messages from Alice should have arrived at Clara
        Assertions.assertTrue(senderEraShouldExist(claraTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, TIGER_URI, 0));
        Assertions.assertTrue(senderEraShouldExist(claraTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, ANIMAL_URI, 1));
        Assertions.assertTrue(senderEraShouldExist(claraTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, HELLO_URI, 2));
        Assertions.assertTrue(senderEraShouldExist(claraTestPeer, EXAMPLE_APP_FORMAT, TestConstants.ALICE_ID, ANIMAL_URI, 3));

        // all messages from Bob, which Alice had previously received, should have arrived at Clara
        // BUG: only the first message is routed
        Assertions.assertTrue(senderEraShouldExist(claraTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, ANIMAL_URI, 0));
        Assertions.assertTrue(senderEraShouldExist(claraTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, ELEPHANT_URI, 1));
        Assertions.assertTrue(senderEraShouldExist(claraTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, ANIMAL_URI, 2));
        Assertions.assertTrue(senderEraShouldExist(claraTestPeer, EXAMPLE_APP_FORMAT, TestConstants.BOB_ID, HELLO_URI, 3));
    }

    public void simpleEncounterWithMessageExchange(String uriAlice, String uriBob)
            throws IOException, ASAPException, InterruptedException {
        this.simpleEncounterWithMessageExchange(uriAlice, uriBob, 0);
    }

    // send messages with given uri, starts and then stops the encounter
    // message content is irrelevant, we don't test for it
    public void simpleEncounterWithMessageExchange(String uriAlice, String uriBob, int encounterNumber)
            throws IOException, ASAPException, InterruptedException {

        simpleEncounterWithMessageExchange(aliceTestPeer, bobTestPeer, uriAlice, uriBob, encounterNumber);
    }

    // send messages with given uri, starts and then stops the encounter
    // message content is irrelevant, we don't test for it
    public void simpleEncounterWithMessageExchange(ASAPTestPeerFS peerA, ASAPTestPeerFS peerB, String uriAlice, String uriBob, int encounterNumber)
            throws IOException, ASAPException, InterruptedException {

        // simulate ASAP first encounter with full ASAP protocol stack and engines
        System.out.println("+++++++++++++++++++ encounter #" + encounterNumber + " starts soon ++++++++++++++++++++");
        Thread.sleep(50);

        // setup message received listener - this should be replaced with your code - you implement a listener.
        ASAPMessageReceivedListenerExample aliceMessageReceivedListenerExample =
                new ASAPMessageReceivedListenerExample();

        peerA.addASAPMessageReceivedListener(EXAMPLE_APP_FORMAT, aliceMessageReceivedListenerExample);

        // example - this should be produced by your application
        byte[] serializedData = TestUtils.serializeExample(42, "from alice", true);

        peerA.sendASAPMessage(EXAMPLE_APP_FORMAT, uriAlice, serializedData);

        ///////////////// BOB //////////////////////////////////////////////////////////////

        // this should be replaced with your code - you must implement a listener.
        ASAPMessageReceivedListenerExample asapMessageReceivedListenerExample =
                new ASAPMessageReceivedListenerExample();

        // register your listener (or that mock) with asap connection mock
        peerB.addASAPMessageReceivedListener(EXAMPLE_APP_FORMAT, asapMessageReceivedListenerExample);

        // bob writes something
        peerB.sendASAPMessage(EXAMPLE_APP_FORMAT, uriBob,
                TestUtils.serializeExample(43, "from bob", false));
        peerB.sendASAPMessage(EXAMPLE_APP_FORMAT, uriBob,
                TestUtils.serializeExample(44, "from bob again", false));

        // give your app a moment to process
        Thread.sleep(500);
        // start actual encounter
        peerA.startEncounter(TestHelper.getPortNumber(), peerB);

        // give your app a moment to process
        Thread.sleep(1000);
        // stop encounter
        peerB.stopEncounter(peerA);
        // give your app a moment to process
        Thread.sleep(1000);
    }

    public boolean senderEraShouldExist(ASAPPeer peer, String format, String sender, String uri, int era)
            throws IOException, ASAPException {
        return peer.getASAPStorage(format).getExistingIncomingStorage(sender).getChunkStorage().existsChunk(uri, era);
    }
}
