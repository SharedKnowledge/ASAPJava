package net.sharksystem.asap.encounter;

import net.sharksystem.utils.tcp.SocketFactory;
import net.sharksystem.utils.testsupport.TestHelper;
import net.sharksystem.asap.*;
import net.sharksystem.asap.apps.testsupport.TestASAPConnectionHandler;
import net.sharksystem.utils.streams.StreamPair;
import net.sharksystem.utils.streams.StreamPairImpl;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class EncounterManagerTests {

    @Test
    public void usage1() throws ASAPException, IOException, InterruptedException {
        ASAPConnectionHandler aliceASAPConnectionHandler =
                new TestASAPConnectionHandler(); // would be a peer

        ASAPEncounterManagerImpl aliceASAPEncounterManager = new ASAPEncounterManagerImpl(aliceASAPConnectionHandler);

        ASAPConnectionHandler bobASAPConnectionHandler =
                new TestASAPConnectionHandler(); // would be a peer

        ASAPEncounterManagerImpl bobASAPEncounterManager = new ASAPEncounterManagerImpl(bobASAPConnectionHandler);

        // Bob connects with Alice
        int alicePortNumber = TestHelper.getPortNumber();
        ServerSocket aliceSrvSocket = new ServerSocket(alicePortNumber);
        SocketFactory aliceSocketFactory = new SocketFactory(aliceSrvSocket);
        new Thread(aliceSocketFactory).start();
        Thread.sleep(42);

        Socket bob2Alice = new Socket("localhost", alicePortNumber);
        // connected
        String b2aRemoteAddress = SocketFactory.getRemoteAddress(bob2Alice);

        StreamPair bob2AliceStreamPair = StreamPairImpl.getStreamPairWithEndpointAddress(
                bob2Alice.getInputStream(),
                bob2Alice.getOutputStream(),
                b2aRemoteAddress);


        String a2bRemoteAddress = aliceSocketFactory.getRemoteAddress();
        StreamPair alice2BobStreamPair = StreamPairImpl.getStreamPairWithEndpointAddress(
                aliceSocketFactory.getInputStream(),
                aliceSocketFactory.getOutputStream(),
                a2bRemoteAddress);

        // tell encounter manager
        aliceASAPEncounterManager.handleEncounter(alice2BobStreamPair, ASAPEncounterConnectionType.INTERNET);
        bobASAPEncounterManager.handleEncounter(bob2AliceStreamPair, ASAPEncounterConnectionType.INTERNET);

    }
}
