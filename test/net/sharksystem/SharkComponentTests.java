package net.sharksystem;

import org.junit.Test;

public class SharkComponentTests {
    static final CharSequence ALICE = "Alice";
    static final CharSequence BOB = "Bob";
    static final CharSequence ROOTFOLDER = "sharkComponent";
    static final CharSequence ALICE_ROOTFOLDER = ROOTFOLDER + "/" + ALICE;
    static final CharSequence YOUR_APP_NAME = "yourAppName";
    static final CharSequence YOUR_URI = "yourSchema://example";

    @Test
    public void usage1() throws SharkException {
        SharkPeerFS sPeer = new SharkPeerFS(ALICE, ALICE_ROOTFOLDER);
        YourComponentFactory factory = new YourComponentFactory();
        Class facadeClass = YourComponent.class;
        sPeer.addComponent(factory, facadeClass);

        sPeer.getComponent(YourComponent.class);
        sPeer.start();
    }

}
