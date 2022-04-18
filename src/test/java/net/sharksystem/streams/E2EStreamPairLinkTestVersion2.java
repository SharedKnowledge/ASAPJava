package net.sharksystem.streams;

import net.sharksystem.asap.apps.testsupport.SocketFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class E2EStreamPairLinkTestVersion2 {

    private static int portnumber = 7777;

    private static int getPortNumber() {
        return E2EStreamPairLinkTestVersion2.portnumber++;
    }

    /**
     * Rebuild connection like in HubGeneric
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void streamPairStreamLinkTest() throws IOException, InterruptedException {

        WrappedStreamPairListenerDummy streamPairListenerDummy = new WrappedStreamPairListenerDummy();

        //////////////////////// ALICE CONNECTOR
        /* side Alice: peer side < ---- > connector side  */
        int port = getPortNumber();
        SocketFactory socketFactory = new SocketFactory(new ServerSocket(port));
        (new Thread(socketFactory)).start();
        Thread.sleep(100); // give it a moment to start

        Socket socket1 = new Socket("localhost", port);
        InputStream connectorPeerSideA_IS = socket1.getInputStream();
        OutputStream connectorPeerSideA_OS = socket1.getOutputStream();
        StreamPairWrapper streamPairConnectorPeerSideAlice =
                new StreamPairWrapper(connectorPeerSideA_IS, connectorPeerSideA_OS,
                        streamPairListenerDummy, TestConstants.ALICE_ID + "(peer side)");

        InputStream connectorHubSideA_IS = socketFactory.getInputStream();
        OutputStream connectorHubSideA_OS = socketFactory.getOutputStream();
        StreamPairWrapper streamPairConnectorHubSideAlice =
                new StreamPairWrapper(connectorHubSideA_IS, connectorHubSideA_OS,
                        streamPairListenerDummy, TestConstants.ALICE_ID + "(hub side)");

        //////////////////////// BOB CONNECTOR
        /* side Bob: peer side < ---- > connector side  */
        port = getPortNumber();
        socketFactory = new SocketFactory(new ServerSocket(port));
        (new Thread(socketFactory)).start();
        Thread.sleep(100); // give it a moment to start

        socket1 = new Socket("localhost", port);
        InputStream connectorPeerSideB_IS = socket1.getInputStream();
        OutputStream connectorPeerSideB_OS = socket1.getOutputStream();
        StreamPairWrapper streamPairConnectorPeerSideBob =
                new StreamPairWrapper(connectorPeerSideB_IS, connectorPeerSideB_OS,
                        streamPairListenerDummy, TestConstants.BOB_ID + "(peer side)");

        InputStream connectorHubSideB_IS = socketFactory.getInputStream();
        OutputStream connectorHubSideB_OS = socketFactory.getOutputStream();
        StreamPairWrapper streamPairConnectorHubSideBob =
                new StreamPairWrapper(connectorHubSideB_IS, connectorHubSideB_OS,
                        streamPairListenerDummy, TestConstants.BOB_ID + "(hub side)");

        //////////////////////// LINK CONNECTOR HUB SIDE
        StreamPairLink dataLink =
                new StreamPairLink(
                        streamPairConnectorHubSideAlice, TestConstants.ALICE_ID,
                        streamPairConnectorHubSideBob, TestConstants.BOB_ID);

        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        //                                      run End-to-End data session                                    //
        /////////////////////////////////////////////////////////////////////////////////////////////////////////

        // simulate protocol
        System.out.println("+++++++++++++++++ simulate protocol ++++++++++++++++++");
        int rounds = 5;

        // peer side Alice
        DataExchangeTester aliceDataSession =
                new DataExchangeTester(
                        streamPairConnectorPeerSideAlice.getInputStream(),
                        streamPairConnectorPeerSideAlice.getOutputStream(),
                        rounds, TestConstants.ALICE_ID);

        // peer side Bob
        DataExchangeTester bobDataSession =
                new DataExchangeTester(
                        streamPairConnectorPeerSideBob.getInputStream(),
                        streamPairConnectorPeerSideBob.getOutputStream(),
                        rounds, TestConstants.BOB_ID);

        Thread aliceDataSessionThread = new Thread(aliceDataSession);
        Thread bobDataSessionThread = new Thread(bobDataSession);

        aliceDataSessionThread.start();
        bobDataSessionThread.start();

        // wait a moment
        Thread.sleep(1000);
        Assert.assertTrue(aliceDataSession.synced);
        Assert.assertTrue(bobDataSession.synced);
        System.out.println(streamPairListenerDummy);

        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        //                                      simulate data session time out                                 //
        /////////////////////////////////////////////////////////////////////////////////////////////////////////

        streamPairConnectorPeerSideAlice.close();
        streamPairConnectorPeerSideBob.close();
        streamPairConnectorHubSideAlice.close();
        streamPairConnectorHubSideBob.close();

        // wait a moment
        System.out.flush();
        Thread.sleep(1000);

        System.out.println("available (Alice (peer)): " + connectorPeerSideA_IS.available());
        System.out.println("available (Alice (hub)): " + connectorHubSideA_IS.available());
        System.out.println("available (Bob (peer)): " + connectorPeerSideB_IS.available());
        System.out.println("available (Bob (hub)): " + connectorHubSideB_IS.available());
        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        //                             run communication over non wrapped streams                              //
        /////////////////////////////////////////////////////////////////////////////////////////////////////////

        System.out.println("******************** ALICE CONNECTOR STILL ALIVE? ********************");
        // connector streams still intact? run session on non wrapped connections
        // Alice
        DataExchangeTester peerSide =
                new DataExchangeTester(
                        connectorPeerSideA_IS, connectorPeerSideA_OS,
                        rounds, TestConstants.ALICE_ID + "(peer)");
        DataExchangeTester hubSide =
                new DataExchangeTester(
                        connectorHubSideA_IS, connectorHubSideA_OS,
                        rounds, TestConstants.ALICE_ID + "(hub)");

        Thread peerDataSessionThread = new Thread(peerSide);
        Thread hubDataSessionThread = new Thread(hubSide);

        peerDataSessionThread.start();
        hubDataSessionThread.start();

        // wait a moment
        Thread.sleep(1000);
        Assert.assertTrue(peerSide.synced);
        Assert.assertTrue(hubSide.synced);

        System.out.println("******************** BOB CONNECTOR STILL ALIVE? ********************");
        peerSide = new DataExchangeTester(
                        connectorPeerSideB_IS, connectorPeerSideB_OS,
                        rounds, TestConstants.BOB_ID + "(peer)");
        hubSide = new DataExchangeTester(
                        connectorHubSideB_IS, connectorHubSideB_OS,
                        rounds, TestConstants.BOB_ID + "(hub)");

        peerDataSessionThread = new Thread(peerSide);
        hubDataSessionThread = new Thread(hubSide);

        peerDataSessionThread.start();
        hubDataSessionThread.start();

        // wait a moment
        Thread.sleep(1000);
        Assert.assertTrue(peerSide.synced);
        Assert.assertTrue(hubSide.synced);
    }

    class WrappedStreamPairListenerDummy implements WrappedStreamPairListener {
        public Map<String, Integer> actions =  new HashMap<>();

        @Override
        public void notifyClosed(StreamPair closedStreamPair, String key) {
            System.out.println("closed: " + key);
        }

        @Override
        public void notifyAction(String key) {
            int newCounter = 0;
            Integer counter = this.actions.get(key);
            if(counter != null) {
                newCounter = counter;
            }
            newCounter++;
            this.actions.put(key, newCounter);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();

            if(this.actions.isEmpty()) {
                sb.append("empty");
            } else {
                for(String key : this.actions.keySet()) {
                    sb.append("actions(");
                    sb.append(key);
                    sb.append("): ");
                    sb.append(this.actions.get(key));
                    sb.append("\n");
                }
            }

            return sb.toString();
        }
    }
}
