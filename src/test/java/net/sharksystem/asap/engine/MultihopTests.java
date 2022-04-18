package net.sharksystem.asap.engine;

import net.sharksystem.CountsReceivedMessagesListener;
import net.sharksystem.asap.apps.testsupport.TestConstants;
import net.sharksystem.asap.apps.testsupport.TestHelper;
import net.sharksystem.asap.ASAPChannel;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPChunkStorage;
import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.asap.apps.testsupport.ASAPTestPeerFS;
import net.sharksystem.asap.cmdline.CmdLineUI;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class MultihopTests {
    /**
     * Create three storages and engine and let hop one message from a to c
     * @throws IOException
     * @throws ASAPException
     * @throws InterruptedException
     */
    //@Test // keep it as an example of batch processor based test case
    public void twoHops() throws IOException, ASAPException, InterruptedException {
        CmdLineUI ui = new CmdLineUI(System.out);

        ui.doResetASAPStorages();

        ui.doCreateASAPPeer("Alice");
        ui.doCreateASAPPeer("Bob");
        ui.doCreateASAPPeer("Clara");

        // create storages
        ui.doCreateASAPApp("Alice twoHops");
        ui.doCreateASAPApp("Bob twoHops");
        ui.doCreateASAPApp("Clara twoHops");

        ui.doSetSendReceivedMessage("Alice:twoHops on");
        ui.doSetSendReceivedMessage("Bob:twoHops on");
        ui.doSetSendReceivedMessage("Clara:twoHops on");

        // add message to alice storage
        String messageAlice2Clara = "HiClara";
        String parameters = "Alice twoHops sn2://abc " + messageAlice2Clara;
        ui.doCreateASAPMessage(parameters);

        // remember Alice' era
        ASAPInternalStorage aliceStorage = this.getFreshStorageByName(ui, "Alice:twoHops");
        int aliceEraWhenIssuedMessage = aliceStorage.getEra();

        System.out.println("**************************************************************************");
        System.out.println("**                       connect Alice with Bob                         **");
        System.out.println("**************************************************************************");
        // connect alice with bob
        ui.doOpen("7070 Alice");
        // wait a moment to give server socket time to be created
        Thread.sleep(10);

        ui.doConnect("7070 Bob");

        // alice should be in era 1 (content has changed before connection) and bob era is 0 - no changes

        // wait a moment
        Thread.sleep(1000);

        // kill connections
        ui.doKill("all");

        // alice should stay in era 1 (no content change), bob should be in era 1 received something

        // wait a moment
        Thread.sleep(1000);

        System.out.println("**************************************************************************");
        System.out.println("**                       connect Bob with Clara                         **");
        System.out.println("**************************************************************************");
        ui.doOpen("8080 Clara");
        // wait a moment to give server socket time to be created
        Thread.sleep(10);
        ui.doConnect("8080 Bob");

        // bob should remain in era 1 o changes, clara is era 0

        // wait a moment
        Thread.sleep(1000);
        // kill connections
        ui.doKill("all");
        // wait a moment
        Thread.sleep(1000);

        // get Clara storage
        ASAPInternalStorage clara = this.getFreshStorageByName(ui, "Clara:twoHops");

        /* message was actually from Bob but originated from Alice. It is put
        into a incoming folder as it would have been directly received from Alice.
        Signatures would allow ensuring if origin was really who mediator claims to be.
        */
        ASAPChunkStorage claraAlice = clara.getReceivedChunksStorage("Alice");

        // clara era was increased after connection terminated - message from bob is in era before current one
//        int eraToLook = ASAPEngine.previousEra(clara.getEra());
        ASAPInternalChunk claraABCChat = claraAlice.getChunk("sn2://abc", aliceEraWhenIssuedMessage);
        CharSequence message = claraABCChat.getMessagesAsCharSequence().next();
        boolean same = messageAlice2Clara.equalsIgnoreCase(message.toString());
        Assert.assertTrue(same);
    }

    @Test
    public void connectionWithNoDataExchange() throws IOException, ASAPException, InterruptedException {
        CmdLineUI ui = new CmdLineUI(System.out);

        ui.doResetASAPStorages();

        // create storages
        ui.doCreateASAPApp("Alice silent");
        ui.doCreateASAPApp("Bob silent");

        System.out.println("**************************************************************************");
        System.out.println("**                       connect Alice with Bob                         **");
        System.out.println("**************************************************************************");
        // connect alice with bob
        ui.doCreateASAPPeer("Alice");
        ui.doOpen("7070 Alice");
        // wait a moment to give server socket time to be created
        Thread.sleep(10);
        ui.doCreateASAPPeer("Bob");

        ui.doConnect("7070 Bob");

        // wait a moment
        Thread.sleep(1000);
        System.out.println("**************************************************************************");
        System.out.println("**                       going to kill connection                       **");
        System.out.println("**************************************************************************");

        // kill connections
        ui.doKill("all");
    }

//    @Test
    public void closedChannelTest() throws IOException, ASAPException, InterruptedException {
        CmdLineUI ui = new CmdLineUI(System.out);
        ui.doResetASAPStorages();

        ui.doCreateASAPPeer("Alice");
        ui.doCreateASAPPeer("Bob");
        ui.doCreateASAPPeer("Clara");
        ui.doCreateASAPPeer("David");

        // create app on each peer
        ui.doCreateASAPApp("Alice chat");
        ui.doCreateASAPApp("Bob chat");
        ui.doCreateASAPApp("Clara chat");
        ui.doCreateASAPApp("David chat");

        ui.doSetSendReceivedMessage("Alice:chat on");
        ui.doSetSendReceivedMessage("Bob:chat on");
        ui.doSetSendReceivedMessage("Clara:chat on");
        ui.doSetSendReceivedMessage("David:chat on");

        // create closed channel with Alice
        ui.doCreateASAPChannel(" Alice chat sn2://closedChannel Bob Clara");
        ui.doPrintChannelInformation("Alice chat sn2://closedChannel");

        // add message
        // add message to alice storage
        String messageAlice2Clara = "HiClara";
        String parameters = "Alice chat sn2://closedChannel " + messageAlice2Clara;
        ui.doCreateASAPMessage(parameters);

        // remember Alice' era
        ASAPInternalStorage aliceStorage = this.getFreshStorageByName(ui, "Alice:chat");
        int aliceEraWhenIssuedMessage = aliceStorage.getEra();

        System.out.println("**************************************************************************");
        System.out.println("**                       connect Alice with Bob                         **");
        System.out.println("**************************************************************************");
        // connect alice with bob
        ui.doOpen("7070 Alice");
        // wait a moment to give server socket time to be created
        Thread.sleep(10);
        ui.doConnect("7070 Bob");

        // alice should be in era 1 (content has changed before connection) and bob era is 0 - no changes
        // wait a moment
        Thread.sleep(1000);
        // kill connections
        ui.doKill("all");
        // alice should stay in era 1 (no content change), bob should be in era 1 received something
        // wait a moment
        Thread.sleep(1000);
        // Bob should now have created an closed asap storage with three recipients
        ASAPInternalStorage bobStorage = this.getFreshStorageByName(ui, "Bob:chat");

        ui.doPrintChannelInformation("Bob chat sn2://closedChannel");

        ASAPChannel bobClosedChannel = bobStorage.getChannel("sn2://closedChannel");
        Set<CharSequence> recipientsList = bobClosedChannel.getRecipients();
        boolean aliceFound = false;
        boolean bobFound = false;
        boolean claraFound = false;
        for(CharSequence recipient : recipientsList) {
            String recipientString = recipient.toString();
            switch(recipient.toString()) {
                case "Alice": aliceFound = true; break;
                case "Bob": bobFound = true; break;
                case "Clara": claraFound = true; break;
                default: Assert.fail("found unexpected recipient: " + recipient);
            }
        }

        // closed channel created with all recipients and owner?
        Assert.assertTrue(aliceFound && bobFound && claraFound);
        Assert.assertTrue(bobClosedChannel.getOwner().toString().equalsIgnoreCase("Alice"));

        // message received?
        ASAPChunkStorage bobAlice = bobStorage.getReceivedChunksStorage("Alice");
        // clara era was increased after connection terminated - message from bob is in era before current one
        ASAPInternalChunk bobABCChat = bobAlice.getChunk("sn2://closedChannel", aliceEraWhenIssuedMessage);
        CharSequence message = bobABCChat.getMessagesAsCharSequence().next();
        Assert.assertTrue(messageAlice2Clara.equalsIgnoreCase(message.toString()));

        System.out.println("**************************************************************************");
        System.out.println("**                       connect Bob with Clara                         **");
        System.out.println("**************************************************************************");
        // connect alice with bob
        ui.doOpen("7071 Bob");
        // wait a moment to give server socket time to be created
        Thread.sleep(10);
        ui.doConnect("7071 Clara");

        // alice should be in era 1 (content has changed before connection) and bob era is 0 - no changes
        // wait a moment
        Thread.sleep(1000);
        // kill connections
        ui.doKill("all");
        // alice should stay in era 1 (no content change), bob should be in era 1 received something
        // wait a moment
        Thread.sleep(1000);
        // Bob should now have created an closed asap storage with three recipients
        ASAPInternalStorage claraStorage = this.getFreshStorageByName(ui, "Clara:chat");

        ui.doPrintChannelInformation("Clara chat sn2://closedChannel");

        ASAPChannel claraClosedChannel = bobStorage.getChannel("sn2://closedChannel");
        recipientsList = bobClosedChannel.getRecipients();
        aliceFound = false;
        bobFound = false;
        claraFound = false;
        for(CharSequence recipient : recipientsList) {
            String recipientString = recipient.toString();
            switch(recipient.toString()) {
                case "Alice": aliceFound = true; break;
                case "Bob": bobFound = true; break;
                case "Clara": claraFound = true; break;
                default: Assert.fail("found unexpected recipient: " + recipient);
            }
        }

        // closed channel created with all recipients and owner?
        Assert.assertTrue(aliceFound && bobFound && claraFound);
        Assert.assertTrue(bobClosedChannel.getOwner().toString().equalsIgnoreCase("Alice"));

        // message received?
        ASAPChunkStorage claraAlice = claraStorage.getReceivedChunksStorage("Alice");
        // clara era was increased after connection terminated - message from bob is in era before current one
        ASAPInternalChunk claraABCChat = claraAlice.getChunk("sn2://closedChannel", aliceEraWhenIssuedMessage);
        message = claraABCChat.getMessagesAsCharSequence().next();
        Assert.assertTrue(messageAlice2Clara.equalsIgnoreCase(message.toString()));

        System.out.println("**************************************************************************");
        System.out.println("**                       connect Clara with David                       **");
        System.out.println("**************************************************************************");
        // connect alice with bob
        ui.doOpen("7072 Clara");
        // wait a moment to give server socket time to be created
        Thread.sleep(10);
        ui.doConnect("7072 David");

        // wait a moment
        Thread.sleep(1000);
        // kill connections
        ui.doKill("all");
        // wait a moment
        Thread.sleep(1000);
        // Bob should now have created an closed asap storage with three recipients
        ASAPInternalStorage davidStorage = this.getFreshStorageByName(ui, "David:chat");

        Assert.assertFalse(davidStorage.channelExists("sn2://closedChannel"));
    }

    private ASAPInternalStorage getFreshStorageByName(CmdLineUI ui, String storageName) throws ASAPException, IOException {
        String rootFolder = ui.getEngineRootFolderByStorageName(storageName);
        return ASAPEngineFS.getExistingASAPEngineFS(rootFolder);
    }

    /**
     * 1) Alice creates two messages in era 0
     * 2) Alice meets Bob. Alice:0->1, Bob:0. Alice:0 (2) -> Bob (two messages era A:0 received by Bob)
     * 3) Bob meets Clara. Bob:0, Clara:0. Alice:0 (2) -> Bob -> Clara (two messages routed to Clara)
     * 4) Alice meets Clara. Alice:1, Clara:0. no exchange: Clara is in sync with Alice:0, Clara has no messages at all
     * 5) Alice creates another message in A:1
     * 6) Alice meets Clara. Alice:1->2, Clara:0; Alice:1 (2) -> Clara (one message era A:1 received by Clara)
     * 7) Bob meets Clara. Bob:0, Clara:0. Alice:1 (1) -> Clara -> Bob (one message routed to Bob)
     */
    @Test
    public void asapRoutingIsFiniteAndCheckEra() throws IOException, ASAPException, InterruptedException {
        /////////////////////////////////// setup test environment
        String aliceFolder = TestHelper.getFullRootFolderName(TestConstants.ALICE_ID, MultihopTests.class);
        aliceFolder = TestHelper.getFullTempFolderName(aliceFolder, false);
        ASAPEngineFS.removeFolder(aliceFolder);

        String bobFolder = TestHelper.getFullRootFolderName(TestConstants.BOB_ID, MultihopTests.class);
        bobFolder = TestHelper.getFullTempFolderName(bobFolder, false);
        ASAPEngineFS.removeFolder(bobFolder);

        String claraFolder = TestHelper.getFullRootFolderName(TestConstants.CLARA_ID, MultihopTests.class);
        claraFolder = TestHelper.getFullTempFolderName(claraFolder, true);
        ASAPEngineFS.removeFolder(claraFolder);

        String appName = TestHelper.produceTestAppName(MultihopTests.class);
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(appName);

        ////////////////////////////////////// setup test peers
        // ALICE
        ASAPTestPeerFS alicePeer = new ASAPTestPeerFS(TestConstants.ALICE_ID, aliceFolder, formats);
        alicePeer.setASAPRoutingAllowed(appName, true);
        CountsReceivedMessagesListener aliceListener = new CountsReceivedMessagesListener(TestConstants.ALICE_ID);
        alicePeer.addASAPMessageReceivedListener(appName, aliceListener);
        // BOB
        ASAPTestPeerFS bobPeer = new ASAPTestPeerFS(TestConstants.BOB_ID, bobFolder, formats);
        bobPeer.setASAPRoutingAllowed(appName, true);
        CountsReceivedMessagesListener bobListener = new CountsReceivedMessagesListener(TestConstants.BOB_ID);
        bobPeer.addASAPMessageReceivedListener(appName, bobListener);
        // CLARA
        ASAPTestPeerFS claraPeer = new ASAPTestPeerFS(TestConstants.CLARA_ID, claraFolder, formats);
        claraPeer.setASAPRoutingAllowed(appName, true);
        CountsReceivedMessagesListener claraListener = new CountsReceivedMessagesListener(TestConstants.CLARA_ID);
        claraPeer.addASAPMessageReceivedListener(appName, claraListener);

        //////////////////////////////////// Alice creates messages
        Thread.sleep(500); // let write logs
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> write two messages <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        byte[] testMessage1 = TestHelper.produceTestMessage();
        byte[] testMessage2 = TestHelper.produceTestMessage();
        ASAPStorage aliceAppStorage = alicePeer.getASAPStorage(appName);
        aliceAppStorage.add(TestConstants.URI, testMessage1);
        aliceAppStorage.add(TestConstants.URI, testMessage2);

        //////////////////////////////////// Alice meets Bob - first exchange: Alice:0 (2) -> Bob
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> Alice meets Bob <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        alicePeer.startEncounter(TestHelper.getPortNumber(), bobPeer);
        // give your app a moment to process
        Thread.sleep(500);
        alicePeer.stopEncounter(bobPeer);
        Thread.sleep(500);
        Assert.assertEquals(1, bobListener.numberOfMessages);

        // check local eras
        // one change after connection establishment
        Assert.assertEquals(1, alicePeer.getASAPStorage(appName).getEra());
        // no change - receiving message does not change local understanding of an era
        Assert.assertEquals(0, bobPeer.getASAPStorage(appName).getEra());

        //////////////////////////////////// Bob meets Clara - routing Alice:0 (2) -> Bob -> Clara
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> Bob meets Clara <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        claraPeer.startEncounter(TestHelper.getPortNumber(), bobPeer);
        // give your app a moment to process
        Thread.sleep(500);
        claraPeer.stopEncounter(bobPeer);
        Thread.sleep(500);
        Assert.assertEquals(1, claraListener.numberOfMessages);

        // no change - receiving message does not change local understanding of an era
        Assert.assertEquals(0, bobPeer.getASAPStorage(appName).getEra());
        Assert.assertEquals(0, claraPeer.getASAPStorage(appName).getEra());

        //////////////////////////////////// Alice meets Clara - nothing: Alice:0 (2) -X Clara already got this
        // reset counter on clara side
        claraListener.numberOfMessages = 0;
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>> Alice meets Clara <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        alicePeer.startEncounter(TestHelper.getPortNumber(), claraPeer);
        // give your app a moment to process
        Thread.sleep(500);
        alicePeer.stopEncounter(claraPeer);
        Thread.sleep(500);
        Assert.assertEquals(0, claraListener.numberOfMessages);

        // no change - receiving message does not change local understanding of an era
        Assert.assertEquals(1, alicePeer.getASAPStorage(appName).getEra());
        Assert.assertEquals(0, claraPeer.getASAPStorage(appName).getEra());

        //////////////////////////////////// Alice creates another message
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> Alice writes another messages <<<<<<<<<<<<<<<<<<");
        byte[] testMessage3 = TestHelper.produceTestMessage();
        aliceAppStorage.add(TestConstants.URI, testMessage3);


        //////////////////////////////////// Alice meets Clara again Alice:1 (1) -> Clara
        // reset counter on clara side
        claraListener.numberOfMessages = 0;
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>> Alice meets Clara again <<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        alicePeer.startEncounter(TestHelper.getPortNumber(), claraPeer);
        // give your app a moment to process
        Thread.sleep(1000);
        alicePeer.stopEncounter(claraPeer);
        Thread.sleep(1000);
        Assert.assertEquals(1, claraListener.numberOfMessages);

        // Alice:1->2 new connection after new data added to channel.
        Assert.assertEquals(2, alicePeer.getASAPStorage(appName).getEra());
        // no change - receiving message does not change local understanding of an era
        Assert.assertEquals(0, claraPeer.getASAPStorage(appName).getEra());
    }
}
