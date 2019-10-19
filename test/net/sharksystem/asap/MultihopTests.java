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
        ui.doCreateASAPStorage("Alice twoHops");
        ui.doCreateASAPStorage("Bob twoHops");
        ui.doCreateASAPStorage("Clara twoHops");

        ui.doSetSendReceivedMessage("Alice:twoHops on");
        ui.doSetSendReceivedMessage("Bob:twoHops on");
        ui.doSetSendReceivedMessage("Clara:twoHops on");

        // add message to alice storage
        String messageAlice2Clara = "HiClara";
        String parameters = "Alice twoHops abcChat " + messageAlice2Clara;
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
        ASAPChunk claraABCChat = claraBob.getChunk("abcChat", eraToLook);
        CharSequence message = claraABCChat.getMessages().next();
        boolean same = messageAlice2Clara.equalsIgnoreCase(message.toString());
        Assert.assertTrue(same);
    }

    //@Test
    public void createNonOpenStorage() throws IOException, ASAPException, InterruptedException {
        CmdLineUI ui = new CmdLineUI(System.out);

        ui.doResetASAPStorages();

        // Alice creates storage with three recipients
        ui.doCreateASAPStorage("Alice nonOpen");
        ui.doCreateASAPStorage("Bob nonOpen");
        ui.doCreateASAPStorage("Clara nonOpen");

        ui.doAddRecipient("Alice:nonOpen Bob");
        ui.doAddRecipient("Alice:nonOpen Clara");
        ui.doAddRecipient("Alice:nonOpen David");

        // message shall reach Bob and Clara but not David
        ui.doAddRecipient("Bob:nonOpen Clara");

        ui.doSetSendReceivedMessage("Alice:nonOpen on");
        ui.doSetSendReceivedMessage("Bob:nonOpen on");

        // add message to alice storage
        String messageAlice2Clara = "HiClara";
        String parameters = "Alice nonOpen abcChat " + messageAlice2Clara;
        ui.doCreateASAPMessage(parameters);

        // message is no in Alice storage

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
        ASAPStorage bobStorage = this.getFreshStorageByName(ui, "Bob:nonOpen");

        CharSequence uri = bobStorage.getChannelURIs().get(0);
        List<CharSequence> recipientsList = bobStorage.getRecipients("nonOpen");
        boolean aliceFound = false;
        boolean bobFound = false;
        boolean claraFound = false;
        boolean davidFound = false;
        for(CharSequence recipient : recipientsList) {
            String recipientString = recipient.toString();
            switch(recipient.toString()) {
                case "Alice": aliceFound = true; break;
                case "Bob": bobFound = true; break;
                case "Clara": claraFound = true; break;
                case "David": davidFound = true; break;
                default: Assert.fail("found unexpected recipient: " + recipient);
            }
        }

        Assert.assertTrue(aliceFound && bobFound && claraFound && davidFound);
    }

    //@Test
    public void twoHopsNonOpenStorage() throws IOException, ASAPException, InterruptedException {
        CmdLineUI ui = new CmdLineUI(System.out);

        ui.doResetASAPStorages();

        // create storages
        ui.doCreateASAPStorage("Alice twoHops");
        ui.doCreateASAPStorage("Bob twoHops");
        ui.doCreateASAPStorage("Clara twoHops");
        ui.doCreateASAPStorage("David twoHops");

        ui.doAddRecipient("Alice:twoHops Bob");
        ui.doAddRecipient("Alice:twoHops Clara");
        ui.doAddRecipient("Alice:twoHops David");

        // message shall reach Bob and Clara but not David
        ui.doAddRecipient("Bob:twoHops Clara");

        ui.doSetSendReceivedMessage("Alice:twoHops on");
        ui.doSetSendReceivedMessage("Bob:twoHops on");

        // add message to alice storage
        String messageAlice2Clara = "HiClara";
        String parameters = "Alice twoHops abcChat " + messageAlice2Clara;
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
        System.out.println("**                       connect Bob with David                         **");
        System.out.println("**************************************************************************");
        // connect alice with bob
        ui.doCreateASAPMultiEngine("Bob");
        ui.doOpen("7070 Bob");
        // wait a moment to give server socket time to be created
        Thread.sleep(10);
        ui.doCreateASAPMultiEngine("David");

        ui.doConnect("7070 David");

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
        ASAPStorage clara = this.getFreshStorageByName(ui, "Clara:twoHops");

        /* message was actually from Bob but originated from Alice. It is put
        into a incoming folder as it would have been directly received from Alice.
        Signatures would allow ensuring if origin was really who mediator claims to be.
        */
        ASAPChunkStorage claraBob = clara.getIncomingChunkStorage("Alice");

        // clara era was increased after connection terminated - message from bob is in era before current one
        int eraToLook = ASAPEngine.previousEra(clara.getEra());
        ASAPChunk claraABCChat = claraBob.getChunk("abcChat", eraToLook);
        CharSequence message = claraABCChat.getMessages().next();
        boolean same = messageAlice2Clara.equalsIgnoreCase(message.toString());
        Assert.assertTrue(same);
    }

    private ASAPStorage getFreshStorageByName(CmdLineUI ui, String storageName) throws ASAPException, IOException {
        String rootFolder = ui.getEngineRootFolderByStorageName(storageName);
        return ASAPEngineFS.getExistingASAPEngineFS(rootFolder);
    }
}
