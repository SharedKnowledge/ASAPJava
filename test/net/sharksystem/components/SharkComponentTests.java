package net.sharksystem.components;

import net.sharksystem.*;
import net.sharksystem.asap.*;
import net.sharksystem.asap.utils.DateTimeHelper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class SharkComponentTests {
    static final CharSequence ALICE = "Alice";
    static final CharSequence BOB = "Bob";
    static final CharSequence ROOTFOLDER = "sharkComponent";
    static final CharSequence ALICE_ROOTFOLDER = ROOTFOLDER + "/" + ALICE;
    static final CharSequence BOB_ROOTFOLDER = ROOTFOLDER + "/" + BOB;
    static final CharSequence YOUR_APP_NAME = "yourAppName";
    static final CharSequence YOUR_URI = "yourSchema://example";

    @Test
    public void usage1() throws SharkException, IOException, ASAPException {
        SharkTestPeerFS.removeFolder(ALICE_ROOTFOLDER); // clean previous version before

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

    private YourComponent setupComponent(SharkPeer sharkPeer) throws SharkException {
        // certificate component required
        sharkPeer.addComponent(new YourComponentFactory(), YourComponent.class);
        // get certificate component
        YourComponent yourComponent =
                (YourComponent) sharkPeer.getComponent(YourComponent.class);
        return yourComponent;
    }


    @Test
    public void aliceCreatesBondAsCreditor() throws SharkException, ASAPException, IOException, InterruptedException {
        ////////////// setup Alice
        SharkTestPeerFS.removeFolder(ALICE_ROOTFOLDER);
        SharkTestPeerFS aliceSharkPeer = new SharkTestPeerFS(ALICE, ALICE_ROOTFOLDER);
        YourComponent aliceComponent = this.setupComponent(aliceSharkPeer);
        ExampleYourComponentListener aliceListener = new ExampleYourComponentListener();
        aliceComponent.subscribeYourComponentListener(aliceListener);

        // Start alice peer
        aliceSharkPeer.start();

        ////////////// setup Bob
        SharkTestPeerFS.removeFolder(BOB_ROOTFOLDER);
        SharkTestPeerFS bobSharkPeer = new SharkTestPeerFS(BOB, BOB_ROOTFOLDER);
        YourComponent bobComponent = this.setupComponent(bobSharkPeer);
        ExampleYourComponentListener bobListener = new ExampleYourComponentListener();
        bobComponent.subscribeYourComponentListener(bobListener);

        // Start alice peer
        bobSharkPeer.start();

        // Bob sends a broadcast on uri A
        bobComponent.sendBroadcastMessageA(YourComponent.FORMAT_A, "Hi all listeners of A");

        ///////////////////////////////// Test specific code - make an encounter Alice Bob
        aliceSharkPeer.getASAPTestPeerFS().startEncounter(7777, bobSharkPeer.getASAPTestPeerFS());

        // give them moment to exchange data
        Thread.sleep(2000);
        //Thread.sleep(Long.MAX_VALUE);
        System.out.println("slept a moment");

        ////////////////////////////////// test if anything was ok.
        // Alice should have received Bob broadcast on A
        Assert.assertEquals(1, aliceListener.counterA);
        Assert.assertEquals(0, aliceListener.counterB);
    }
}
