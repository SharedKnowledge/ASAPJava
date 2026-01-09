package bugreports.snm_20;

import net.sharksystem.CountsReceivedMessagesListener;
import net.sharksystem.SharkException;
import net.sharksystem.asap.*;
import net.sharksystem.asap.apps.TCPServerSocketAcceptor;
import net.sharksystem.utils.streams.StreamPairImpl;
import net.sharksystem.utils.testsupport.TestConstants;
import net.sharksystem.utils.testsupport.TestHelper;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;

import static net.sharksystem.utils.testsupport.TestConstants.URI;
import static net.sharksystem.utils.testsupport.TestConstants.TEST_APP_FORMAT;

class SNMReport20 {
    public static final String TEST_FOLDER = "snmReport20";
    private static final int PORTNUMBER = 6907;

    @Test
    public void test2PeersTalkToAlice() throws IOException, SharkException, InterruptedException {
        // supported formats
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(TEST_APP_FORMAT);

        // test folder for this test run
        String rootFolder = TestHelper.getFullTempFolderName(TEST_FOLDER, true);

        ////////////////////////// set up peers
        // set up alice
        String aliceFolder = rootFolder + "/" + TestConstants.ALICE_ID;
        ASAPPeerFS alice = new ASAPPeerFS(TestConstants.ALICE_ID, aliceFolder, formats);
        // set up bob
        String bobFolder = rootFolder + "/" + TestConstants.BOB_ID;
        ASAPPeerFS bob = new ASAPPeerFS(TestConstants.BOB_ID, bobFolder, formats);
        // set up clara
        String claraFolder = rootFolder + "/" + TestConstants.CLARA_ID;
        ASAPPeerFS clara = new ASAPPeerFS(TestConstants.CLARA_ID, claraFolder, formats);

        CountsReceivedMessagesListener aliceReceivedMessagesListener = new CountsReceivedMessagesListener();
        alice.addASAPMessageReceivedListener(TEST_APP_FORMAT, aliceReceivedMessagesListener);

        ////////////////////////// encounter manager
        ASAPEncounterManager aliceEncounterManager = new ASAPEncounterManagerImpl(alice, TestConstants.ALICE_ID);
        ASAPEncounterManager bobEncounterManager = new ASAPEncounterManagerImpl(bob, TestConstants.BOB_ID);
        ASAPEncounterManager claraEncounterManager = new ASAPEncounterManagerImpl(clara, TestConstants.CLARA_ID);

        ////////////////////////// set up server socket and handle connection requests
        int portNumberAlice = TestHelper.getPortNumber();

        // start system on alice' side
        TCPServerSocketAcceptor aliceTcpServerSocketAcceptor =
                new TCPServerSocketAcceptor(portNumberAlice, aliceEncounterManager);

        // give it a moment to settle
        Thread.sleep(5);

        // send message before connecting
        bob.sendASAPMessage(TEST_APP_FORMAT, URI, TestConstants.MESSAGE_BOB_TO_ALICE_1);
        clara.sendASAPMessage(TEST_APP_FORMAT, URI, TestConstants.MESSAGE_CLARA_TO_ALICE_1);

        // open connections to Alice
        Socket socketBob = new Socket("localhost", portNumberAlice);
        Socket socketClara = new Socket("localhost", portNumberAlice);

        // let Bob handle connection to alice
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    bobEncounterManager.handleEncounter(
                            StreamPairImpl.getStreamPair(socketBob.getInputStream(), socketBob.getOutputStream()),
                            ASAPEncounterConnectionType.INTERNET);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        // let Clara handle connection to alice
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    claraEncounterManager.handleEncounter(
                            StreamPairImpl.getStreamPair(socketClara.getInputStream(), socketClara.getOutputStream()),
                            ASAPEncounterConnectionType.INTERNET);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        // send messages in an open connection
        bob.sendASAPMessage(TEST_APP_FORMAT, URI, TestConstants.MESSAGE_BOB_TO_ALICE_2);
        clara.sendASAPMessage(TEST_APP_FORMAT, URI, TestConstants.MESSAGE_CLARA_TO_ALICE_2);

        // give it a moment to run ASAP session
        Thread.sleep(3000);

        Assert.assertEquals(4, aliceReceivedMessagesListener.numberOfMessages);
    }


    @Test
    public void testOnePeerTalkToAlice() throws IOException, SharkException, InterruptedException {
        // supported formats
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(TEST_APP_FORMAT);

        // test folder for this test run
        String rootFolder = TestHelper.getFullTempFolderName(TEST_FOLDER, true);

        ////////////////////////// set up peers
        // set up alice
        String aliceFolder = rootFolder + "/" + TestConstants.ALICE_ID;
        ASAPPeerFS alice = new ASAPPeerFS(TestConstants.ALICE_ID, aliceFolder, formats);
        // set up bob
        String bobFolder = rootFolder + "/" + TestConstants.BOB_ID;
        ASAPPeerFS bob = new ASAPPeerFS(TestConstants.BOB_ID, bobFolder, formats);

        CountsReceivedMessagesListener aliceReceivedMessagesListener = new CountsReceivedMessagesListener();
        alice.addASAPMessageReceivedListener(TEST_APP_FORMAT, aliceReceivedMessagesListener);

        ////////////////////////// encounter manager
        ASAPEncounterManager aliceEncounterManager = new ASAPEncounterManagerImpl(alice, TestConstants.ALICE_ID);
        ASAPEncounterManager bobEncounterManager = new ASAPEncounterManagerImpl(bob, TestConstants.BOB_ID);

        ////////////////////////// set up server socket and handle connection requests
        int portNumberAlice = TestHelper.getPortNumber();

        // start system on alice' side
        TCPServerSocketAcceptor aliceTcpServerSocketAcceptor =
                new TCPServerSocketAcceptor(portNumberAlice, aliceEncounterManager);

        // give it a moment to settle
        Thread.sleep(5);

        // send message before connecting
        bob.sendASAPMessage(TEST_APP_FORMAT, URI, TestConstants.MESSAGE_BOB_TO_ALICE_1);

        // open connections to Alice
        Socket socketBob = new Socket("localhost", portNumberAlice);

        // let Bob handle connection to alice
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    bobEncounterManager.handleEncounter(
                            StreamPairImpl.getStreamPair(socketBob.getInputStream(), socketBob.getOutputStream()),
                            ASAPEncounterConnectionType.INTERNET);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        // send messages in an open connection
        bob.sendASAPMessage(TEST_APP_FORMAT, URI, TestConstants.MESSAGE_BOB_TO_ALICE_2);

        // give it a moment to run ASAP session
        Thread.sleep(3000);

        Assert.assertEquals(2, aliceReceivedMessagesListener.numberOfMessages);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                         splitted test                                                         //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Test
    public void alice() throws IOException, SharkException, InterruptedException {
        // supported formats
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(TEST_APP_FORMAT);

        // test folder for this test run
        String rootFolder = TestHelper.getFullTempFolderName(TEST_FOLDER, true);

        ////////////////////////// set up peer
        // set up alice
        String aliceFolder = rootFolder + "/" + TestConstants.ALICE_ID;
        ASAPPeerFS alice = new ASAPPeerFS(TestConstants.ALICE_ID, aliceFolder, formats);

        CountsReceivedMessagesListener aliceReceivedMessagesListener = new CountsReceivedMessagesListener();
        alice.addASAPMessageReceivedListener(TEST_APP_FORMAT, aliceReceivedMessagesListener);

        ////////////////////////// encounter manager
        ASAPEncounterManager aliceEncounterManager = new ASAPEncounterManagerImpl(alice, TestConstants.ALICE_ID);

        ////////////////////////// set up server socket and handle connection requests
        // start system on alice' side
        TCPServerSocketAcceptor aliceTcpServerSocketAcceptor =
                new TCPServerSocketAcceptor(PORTNUMBER, aliceEncounterManager);

        // give it a moment to settle
        Thread.sleep(5);

        // give it a moment to run ASAP session
        Thread.sleep(60000);

        Assert.assertEquals(4, aliceReceivedMessagesListener.numberOfMessages);
    }

    @Test
    public void bobAndClara2() throws InterruptedException {
        Thread bobThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    peerSends2Alice(TestConstants.BOB_ID,
                            TestConstants.MESSAGE_BOB_TO_ALICE_1, TestConstants.MESSAGE_ALICE_TO_BOB_2);
                } catch (IOException | SharkException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread claraThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    peerSends2Alice(TestConstants.CLARA_ID,
                            TestConstants.MESSAGE_CLARA_TO_ALICE_1, TestConstants.MESSAGE_CLARA_TO_ALICE_2);
                } catch (IOException | SharkException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        bobThread.start();
        claraThread.start();

        Thread.sleep(3000);
    }

    public void peerSends2Alice(String peerID, byte[] msg1, byte[] msg2) throws IOException, SharkException, InterruptedException {
        // supported formats
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(TEST_APP_FORMAT);

        // test folder for this test run
        String rootFolder = TestHelper.getFullTempFolderName(TEST_FOLDER, true);

        ////////////////////////// set up peer
        String peerFolder = rootFolder + "/" + peerID;
        ASAPPeerFS bob = new ASAPPeerFS(peerID, peerFolder, formats);

        ////////////////////////// encounter manager
        ASAPEncounterManager peerEncounterManager = new ASAPEncounterManagerImpl(bob, peerID);

        ////////////////////////// set up server socket and handle connection requests
        bob.sendASAPMessage(TEST_APP_FORMAT, URI, msg1);

        // open connections to Alice
        Socket socket2Alice = new Socket("localhost", PORTNUMBER);

        // let encounter manager handle connection to alice
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>> " + peerID + " 1 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                    peerEncounterManager.handleEncounter(
                            StreamPairImpl.getStreamPair(socket2Alice.getInputStream(), socket2Alice.getOutputStream()),
                            ASAPEncounterConnectionType.INTERNET);
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>  " + peerID + " 2 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        Thread.sleep(1000);
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>  " + peerID + " SENDING 1 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        bob.sendASAPMessage(TEST_APP_FORMAT, URI, msg2);
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>  " + peerID + " SENDING 2 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

        // give it a moment to run ASAP session
        Thread.sleep(3000);
    }

    @Test
    public void bobAndClara() throws IOException, SharkException, InterruptedException {
        // supported formats
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(TEST_APP_FORMAT);

        // test folder for this test run
        String rootFolder = TestHelper.getFullTempFolderName(TEST_FOLDER, true);

        ////////////////////////// set up peers
        // set up bob
        String bobFolder = rootFolder + "/" + TestConstants.BOB_ID;
        ASAPPeerFS bob = new ASAPPeerFS(TestConstants.BOB_ID, bobFolder, formats);
        // set up clara
        String claraFolder = rootFolder + "/" + TestConstants.CLARA_ID;
        ASAPPeerFS clara = new ASAPPeerFS(TestConstants.CLARA_ID, claraFolder, formats);

        ////////////////////////// encounter manager
        ASAPEncounterManager bobEncounterManager = new ASAPEncounterManagerImpl(bob, TestConstants.BOB_ID);
        ASAPEncounterManager claraEncounterManager = new ASAPEncounterManagerImpl(clara, TestConstants.CLARA_ID);

        ////////////////////////// set up server socket and handle connection requests
        bob.sendASAPMessage(TEST_APP_FORMAT, URI, TestConstants.MESSAGE_BOB_TO_ALICE_1);
        clara.sendASAPMessage(TEST_APP_FORMAT, URI, TestConstants.MESSAGE_CLARA_TO_ALICE_1);

        // open connections to Alice
        Socket socketBob = new Socket("localhost", PORTNUMBER);
        Socket socketClara = new Socket("localhost", PORTNUMBER);

        // let Bob handle connection to alice
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>  BOB 1 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                    bobEncounterManager.handleEncounter(
                            StreamPairImpl.getStreamPair(socketBob.getInputStream(), socketBob.getOutputStream()),
                            ASAPEncounterConnectionType.INTERNET);
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>  BOB 2 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        // let Clara handle connection to alice
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>  CLARA 1 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                    claraEncounterManager.handleEncounter(
                            StreamPairImpl.getStreamPair(socketClara.getInputStream(), socketClara.getOutputStream()),
                            ASAPEncounterConnectionType.INTERNET);
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>  CLARA 2 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        Thread.sleep(1000);
        // send messages in an open connection
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>  BOB SENDING 1 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                    bob.sendASAPMessage(TEST_APP_FORMAT, URI, TestConstants.MESSAGE_BOB_TO_ALICE_2);
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>  BOB SENDING 2 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

                } catch (ASAPException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>  CLARA SENDING 1 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                    clara.sendASAPMessage(TEST_APP_FORMAT, URI, TestConstants.MESSAGE_CLARA_TO_ALICE_2);
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>  CLARA SENDING 2 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                } catch (ASAPException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        // give it a moment to run ASAP session
        Thread.sleep(3000);
    }
}
