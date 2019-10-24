package net.sharksystem.asap;

import net.sharksystem.cmdline.CmdLineUI;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

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

        System.out.println("**************************************************************************");
        System.out.println("**                       connect Alice with Bob                         **");
        System.out.println("**************************************************************************");
        // connect alice with bob
        ui.doCreateASAPMultiEngine("Alice");
        ui.doOpen("7070 Alice");
        // wait a moment to give server socket time to be created
        Thread.sleep(10);
        ui.doCreateASAPMultiEngine("Bob");

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
        ui.doCreateASAPMultiEngine("Clara");
        ui.doOpen("8080 Clara");
        // wait a moment to give server socket time to be created
        Thread.sleep(10);
        ui.doConnect("8080 Bob");

        // bob should remain in era 1 o changes, clara is era 0

        // wait a moment
        Thread.sleep(1000);
        // kill connections
        ui.doKill("all");

        // get Clara storage
        String rootFolder = ui.getEngineRootFolderByStorageName("Clara:twoHops");
        ASAPStorage clara = ASAPEngineFS.getExistingASAPEngineFS(rootFolder);

        /* message was actually from Bob but originated from Alice. It is put
        into a incoming folder as it would have been directly received from Alice.
        Signatures would allow ensuring if origin was really who mediator claims to be.
        */
        ASAPChunkStorage claraBob = clara.getIncomingChunkStorage("Alice");

        // clara era was increased after connection terminated - message from bob is in era before current one
        int eraToLook = ASAPEngine.previousEra(clara.getEra());
        ASAPChunk claraABCChat = claraBob.getChunk("sn2://abc", eraToLook);
        CharSequence message = claraABCChat.getMessages().next();
        boolean same = messageAlice2Clara.equalsIgnoreCase(message.toString());
        Assert.assertTrue(same);
    }

    @Test
    public void createNonOpenStorage() throws IOException, ASAPException, InterruptedException {
        CmdLineUI ui = new CmdLineUI(System.out);
        ui.doResetASAPStorages();

        // create app on each peer
        ui.doCreateASAPApp("Alice chat");
        ui.doCreateASAPApp("Bob chat");
        ui.doCreateASAPApp("Clara chat");

        // create closed channel with Alice
        ui.doCreateASAPChannel(" Alice chat sn2://closedChannel Bob Clara");
        ui.doPrintChannelInformation("Alice chat sn2://closedChannel");

        // add message
        // add message to alice storage
        String messageAlice2Clara = "HiClara";
        String parameters = "Alice chat sn2://closedChannel " + messageAlice2Clara;
        ui.doCreateASAPMessage(parameters);

        System.out.println("**************************************************************************");
        System.out.println("**                       connect Alice with Bob                         **");
        System.out.println("**************************************************************************");
        // connect alice with bob
        ui.doCreateASAPMultiEngine("Alice");
        ui.doOpen("7070 Alice");
        // wait a moment to give server socket time to be created
        Thread.sleep(10);
        ui.doCreateASAPMultiEngine("Bob");
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

        List<CharSequence> recipientsList = bobStorage.getRecipients("sn2://closedChannel");
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

        Assert.assertTrue(aliceFound && bobFound && claraFound);

        // check for a message
        /* message was actually from Bob but originated from Alice. It is put
        into a incoming folder as it would have been directly received from Alice.
        Signatures would allow ensuring if origin was really who mediator claims to be.
        */
        ASAPChunkStorage bobAlice = bobStorage.getIncomingChunkStorage("Alice");

        // clara era was increased after connection terminated - message from bob is in era before current one
        int eraToLook = ASAPEngine.previousEra(bobStorage.getEra());
        ASAPChunk bobABCChat = bobAlice.getChunk("sn2://closedChannel", eraToLook);
        CharSequence message = bobABCChat.getMessages().next();
        Assert.assertTrue(messageAlice2Clara.equalsIgnoreCase(message.toString()));
    }

    private ASAPStorage getFreshStorageByName(CmdLineUI ui, String storageName) throws ASAPException, IOException {
        String rootFolder = ui.getEngineRootFolderByStorageName(storageName);
        return ASAPEngineFS.getExistingASAPEngineFS(rootFolder);
    }
}
