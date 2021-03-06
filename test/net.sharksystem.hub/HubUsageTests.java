package net.sharksystem.hub;

import net.sharksystem.TestConstants;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;

import static net.sharksystem.TestConstants.*;

public class HubUsageTests {
    String ROOT_FOLDER = TestConstants.ROOT_DIRECTORY + HubUsageTests.class.getSimpleName() + "/";

    @Test
    public void usage() throws IOException, InterruptedException, ASAPException {
        int specificPort = 6907;
        TCPHub hub = new TCPHub(specificPort);
        hub.setPortRange(7000, 9000); // optional - required to configure a firewall
        hub.start();

        HubConnector aliceHubConnector = new TCPHubConnector(specificPort);
        HubConnectorTester aliceListener = new HubConnectorTester(ALICE_ID);
        aliceHubConnector.setListener(aliceListener);

        aliceHubConnector.connectHub(ALICE_ID);
        Collection<CharSequence> peerNames = aliceHubConnector.getPeerIDs();
        Assert.assertEquals(0, peerNames.size());

        HubConnectorTester bobListener = new HubConnectorTester(BOB_ID);
        HubConnector bobHubConnector = new TCPHubConnector(specificPort);
        bobHubConnector.setListener(bobListener);
        bobHubConnector.connectHub(BOB_ID);

        Thread.sleep(100);

        peerNames = bobHubConnector.getPeerIDs();
        Assert.assertEquals(1, peerNames.size());

        /// Alice meets Bob
        aliceHubConnector.connectPeer(BOB_ID);

        Thread.sleep(1000);
        //Thread.sleep(Long.MAX_VALUE);
        Assert.assertEquals(1, aliceListener.numberNofications());
        Assert.assertEquals(1, bobListener.numberNofications());

        aliceHubConnector.disconnect();
        Thread.sleep(100);

        bobHubConnector.syncHubInformation();
        Thread.sleep(100);

        peerNames = bobHubConnector.getPeerIDs();
        Assert.assertEquals(0, peerNames.size());
    }

    class HubConnectorTester implements NewConnectionListener {
        private final String peerID;
        private int numberNofications = 0;

        public HubConnectorTester(String peerID) {
            this.peerID = peerID;
        }

        @Override
        public void notifyPeerConnected(PeerConnection peerConnection) {
            System.out.println("listener of " + peerID + " got notified about connection from " + peerConnection.peerID);
            this.numberNofications++;

            try {
                //Thread.sleep(1500);
                String message = this.peerID;
                ASAPSerialization.writeCharSequenceParameter(message, peerConnection.os);
                // read
                String receivedMessage = ASAPSerialization.readCharSequenceParameter(peerConnection.is);
                System.out.println(this.peerID + " received: " + receivedMessage);

                Thread.sleep(100);
                message = "Hi: " + receivedMessage;
                ASAPSerialization.writeCharSequenceParameter(message, peerConnection.os);
                receivedMessage = ASAPSerialization.readCharSequenceParameter(peerConnection.is);
                // read
                System.out.println(this.peerID + " received#2: " + receivedMessage);
                peerConnection.is.close();
                peerConnection.os.close();
            } catch (IOException | ASAPException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        public int numberNofications() {
            return this.numberNofications;
        }
    }
}
