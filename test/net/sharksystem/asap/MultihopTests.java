package net.sharksystem.asap;

import net.sharksystem.cmdline.CmdLineUI;
import org.junit.Test;

import java.io.IOException;

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
        ui.doCreateASAPMessage("Alice twoHops abcChat HiClara");

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

        // wait a moment
        Thread.sleep(1000);

        // kill connections
        ui.doKill("all");

        System.out.println("**************************************************************************");
        System.out.println("**                       connect Bob with Clara                         **");
        System.out.println("**************************************************************************");
        ui.doCreateASAPMultiEngine("Clara");
        ui.doOpen("8080 Clara");
        // wait a moment to give server socket time to be created
        Thread.sleep(10);
        ui.doConnect("8080 Bob");

        // wait a moment
        Thread.sleep(1000);
        // kill connections
        ui.doKill("all");

        // get Clara storage
        ASAPStorage clara = ui.getStorage("Clara:twoHops");
        ASAPChunkStorage claraBob = clara.getIncomingChunkStorage("Bob");
        ASAPChunk claraABCChat = claraBob.getChunk("abcChat", clara.getEra());

    }
}
