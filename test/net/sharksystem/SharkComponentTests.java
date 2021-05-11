package net.sharksystem;

import net.sharksystem.asap.*;
import net.sharksystem.asap.engine.ASAPEngineFS;
import net.sharksystem.asap.utils.DateTimeHelper;
import org.junit.Test;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class SharkComponentTests {
    static final CharSequence ALICE = "Alice";
    static final CharSequence BOB = "Bob";
    static final CharSequence ROOTFOLDER = "sharkComponent";
    static final CharSequence ALICE_ROOTFOLDER = ROOTFOLDER + "/" + ALICE;
    static final CharSequence YOUR_APP_NAME = "yourAppName";
    static final CharSequence YOUR_URI = "yourSchema://example";

    @Test
    public void usage1() throws SharkException, IOException, ASAPException {
        ASAPEngineFS.removeFolder(ALICE_ROOTFOLDER.toString()); // clean previous version before

        SharkPeerFS sPeer = new SharkPeerFS(ALICE, ALICE_ROOTFOLDER);
        YourComponentFactory factory = new YourComponentFactory();
        Class facadeClass = YourComponent.class;
        sPeer.addComponent(factory, facadeClass);

        sPeer.getComponent(YourComponent.class);
        sPeer.start();
    }

    @Test
    public void scratch() {
        System.out.println(DateTimeHelper.long2DateString(System.currentTimeMillis()));
        System.out.println(DateTimeHelper.long2ExactTimeString(System.currentTimeMillis()));
        System.out.println(DateFormat.getInstance().format(new Date(System.currentTimeMillis())));
    }

}
