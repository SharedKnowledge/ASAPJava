package net.sharksystem.asap;

import net.sharksystem.cmdline.CmdLineUI;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;

public class MultihopTests {
    /**
     * Create three storages and engine and let hop one message from a to c
     * @throws IOException
     * @throws ASAPException
     * @throws InterruptedException
     */
    @Test
    public void twoHops() throws IOException, ASAPException, InterruptedException {
        CmdLineUI ui = new CmdLineUI(System.out);

        ui.doResetASAPStorages();

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
        ASAPStorage aliceStorage = this.getFreshStorageByName(ui, "Alice:twoHops");
        int aliceEraWhenIssuedMessage = aliceStorage.getEra();

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
        ui.doCreateASAPPeer("Clara");
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
        ASAPStorage clara = this.getFreshStorageByName(ui, "Clara:twoHops");

        /* message was actually from Bob but originated from Alice. It is put
        into a incoming folder as it would have been directly received from Alice.
        Signatures would allow ensuring if origin was really who mediator claims to be.
        */
        ASAPChunkStorage claraAlice = clara.getReceivedChunksStorage("Alice");

        // clara era was increased after connection terminated - message from bob is in era before current one
//        int eraToLook = ASAPEngine.previousEra(clara.getEra());
        ASAPChunk claraABCChat = claraAlice.getChunk("sn2://abc", aliceEraWhenIssuedMessage);
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

    @Test
    public void closedChannelTest() throws IOException, ASAPException, InterruptedException {
        CmdLineUI ui = new CmdLineUI(System.out);
        ui.doResetASAPStorages();

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
        ASAPStorage aliceStorage = this.getFreshStorageByName(ui, "Alice:chat");
        int aliceEraWhenIssuedMessage = aliceStorage.getEra();

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

        // alice should be in era 1 (content has changed before connection) and bob era is 0 - no changes
        // wait a moment
        Thread.sleep(1000);
        // kill connections
        ui.doKill("all");
        // alice should stay in era 1 (no content change), bob should be in era 1 received something
        // wait a moment
        Thread.sleep(1000);
        // Bob should now have created an closed asap storage with three recipients
        ASAPStorage bobStorage = this.getFreshStorageByName(ui, "Bob:chat");

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
        ASAPChunk bobABCChat = bobAlice.getChunk("sn2://closedChannel", aliceEraWhenIssuedMessage);
        CharSequence message = bobABCChat.getMessagesAsCharSequence().next();
        Assert.assertTrue(messageAlice2Clara.equalsIgnoreCase(message.toString()));

        System.out.println("**************************************************************************");
        System.out.println("**                       connect Bob with Clara                         **");
        System.out.println("**************************************************************************");
        // connect alice with bob
        ui.doCreateASAPPeer("Bob");
        ui.doOpen("7071 Bob");
        // wait a moment to give server socket time to be created
        Thread.sleep(10);
        ui.doCreateASAPPeer("Clara");
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
        ASAPStorage claraStorage = this.getFreshStorageByName(ui, "Clara:chat");

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
        ASAPChunk claraABCChat = claraAlice.getChunk("sn2://closedChannel", aliceEraWhenIssuedMessage);
        message = claraABCChat.getMessagesAsCharSequence().next();
        Assert.assertTrue(messageAlice2Clara.equalsIgnoreCase(message.toString()));

        System.out.println("**************************************************************************");
        System.out.println("**                       connect Clara with David                       **");
        System.out.println("**************************************************************************");
        // connect alice with bob
        ui.doCreateASAPPeer("Clara");
        ui.doOpen("7072 Clara");
        // wait a moment to give server socket time to be created
        Thread.sleep(10);
        ui.doCreateASAPPeer("David");
        ui.doConnect("7072 David");

        // wait a moment
        Thread.sleep(1000);
        // kill connections
        ui.doKill("all");
        // wait a moment
        Thread.sleep(1000);
        // Bob should now have created an closed asap storage with three recipients
        ASAPStorage davidStorage = this.getFreshStorageByName(ui, "David:chat");

        Assert.assertFalse(davidStorage.channelExists("sn2://closedChannel"));
    }

    private ASAPStorage getFreshStorageByName(CmdLineUI ui, String storageName) throws ASAPException, IOException {
        String rootFolder = ui.getEngineRootFolderByStorageName(storageName);
        return ASAPEngineFS.getExistingASAPEngineFS(rootFolder);
    }
}
