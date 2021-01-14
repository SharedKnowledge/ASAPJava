package net.sharksystem.asap.appTests;

import net.sharksystem.asap.apps.testsupport.ASAPTestPeerWrapperFS;
import net.sharksystem.asap.ASAPException;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class Reenactment {
    public static final String ALICE_ROOT_FOLDER = "reenact/Alice";
    public static final String ALICE_APP_FOLDER = ALICE_ROOT_FOLDER + "/appFolder";
    public static final String BOB_ROOT_FOLDER = "reenact/Bob";
    public static final String BOB_APP_FOLDER = BOB_ROOT_FOLDER + "/appFolder";
    public static final String ALICE = "Alice";
    public static final String BOB = "Bob";


    @Test
    public void reenact2021_01_05() throws IOException, ASAPException, InterruptedException {
        String format = "application/x-BluetoothSchach";
        String uri1 = "bluetoothschach://";
        String uri2 = "bluetoothschach://3979";
        String oneSign = "X";
        String message = "message B -> A";

        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(format);

        ///////////////// ALICE //////////////////////////////////////////////////////////////
        // setup mocked peer / asap application and activity in android
        ASAPTestPeerWrapperFS aliceSimplePeer = new ASAPTestPeerWrapperFS(ALICE, formats);
        ASAPTestPeerWrapperFS bobSimplePeer = new ASAPTestPeerWrapperFS(BOB, formats);

        aliceSimplePeer.sendASAPMessage(format, uri1, oneSign.getBytes());

        bobSimplePeer.sendASAPMessage(format, uri2, message.getBytes());

        aliceSimplePeer.startEncounter(7777, bobSimplePeer);
        // give your app a moment to process
        Thread.sleep(1000);
        // stop encounter
        bobSimplePeer.stopEncounter(aliceSimplePeer);
        // give your app a moment to process
        Thread.sleep(1000);
    }
}
