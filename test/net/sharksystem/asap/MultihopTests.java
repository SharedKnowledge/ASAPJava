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

        // add message to alice storage
        ui.doCreateASAPMessage("Alice twoHops abcChat HiClara");

        // connect alice with bob
        ui.doCreateASAPMultiEngine("Alice");
        ui.doOpen("7070 Alice");
        ui.doCreateASAPMultiEngine("Bob");

        ui.doConnect("7070 Bob");

        // wait a moment
        Thread.sleep(1000);

        // kill connections
        ui.doKill("all");

        ui.doCreateASAPMultiEngine("Clara");
        ui.doOpen("7070 Clara");
        ui.doConnect("7070 Bob");

        // wait a moment
        Thread.sleep(1000);

        // get Clara storage
        ASAPStorage clara = ui.getStorage("Clara");
        ASAPChunkStorage claraBob = clara.getIncomingChunkStorage("Bob");
        claraBob.getChunk("abcChat", clara.getEra() - 1);
    }
}
