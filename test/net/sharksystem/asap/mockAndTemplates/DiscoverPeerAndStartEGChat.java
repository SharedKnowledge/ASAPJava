package net.sharksystem.asap.mockAndTemplates;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.internals.ASAPMessages;
import net.sharksystem.asap.ASAPEnvironmentChangesListener;
import net.sharksystem.asap.ASAPMessageReceivedListener;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.apps.testsupport.ASAPPeerMock;
import net.sharksystem.asap.apps.testsupport.ASAPTestPeerFS;
import org.junit.Test;

import java.io.*;
import java.util.*;

import static net.sharksystem.asap.mockAndTemplates.TestUtils.ALICE;
import static net.sharksystem.asap.mockAndTemplates.TestUtils.BOB;

/**
 * That's an example. It illustrates how an application can detect a new peer in its surrounding by using
 * the ASAPEnvironmentChangesListener. It send a message to the newly detected peers. There are two test variants:
 * One with the mock, the other with the real asap protocol stack.
 *
 * Some considerations
 * An ASAPEnvironmentChangesListener is called whenever an ASAP peer detects changes in its connections.
 * New connection(s) could have been established or connection(s)) are dropped.
 *
 * For what could such an information be good for?
 * We could initiate a game, a chat, ask to come in a get a special discount for our brand new coffee brand
 * (location based / point of sales advertisement) etc. pp.
 *
 * Interesting question: Should we send a message only to this peer with ASAP?
 *
 * Depends on your apps' security needs. I suggest for most cases:
 * Send an asap message to anybody. Use your application data to describe recipient of this message.
 *
 * Might sound weired at the first glance but consider this: ASAP provides routing in ad-hoc and even scatter nets.
 * It only works if any peer is a potential message recipient and for that reason a router, though.
 * ASAP peer would route messages from known applications (!) if you set a flag.
 *
 * Restrictions on ASAP message recipients are restrictions on ASAP routing capabilities. It is if your would
 * require a very dedicated IP router to transmit your IP packages in Internet.
 *
 * Note: Limiting routing peers is a very crucial feature in ASAP. It allows to set up very high secure applications.
 * Such applications can define what devices (we talk about hardware) are trustworthy. Other devices would not even
 * get notified about any communication. Such applications control their network on a hardware level and not only on
 * a logical level. There is hardly a stronger security thinkable. Drawback: such applications must be aware of that
 * fact and must maintain their network.
 *
 * The gossiping nature of ASAP is very welcome in most cases, though. Any peer becomes a potential router.
 * Your peer will only route messages from applications you explicitly created in the first place.
 * A chat application would not route messages from a chess application if you not explicitly tell otherwise. Moreover:
 *
 * There is a complete public key infrastructure (PKI) in the ASAP universe. Use it if you want to:
 * https://github.com/SharedKnowledge/ASAPCertificateExchange/wiki.
 *
 * That's the suggested way for most applications: Create an application specific data unit.
 * They can also contain the recipient of course. Your messages can be encrypted for the recipient and
 * can be signed by sender.
 *
 * Send those messages to any ASAP peer. ASAP will make the routing and deliver your application messages as soon
 * as possible through an ad-hoc (scatter) network.
 *
 * Following code demonstrates such a first contact behaviour.
 *
 * @author thsc42
 */

public class DiscoverPeerAndStartEGChat {
    public static final CharSequence YOUR_APP_NAME = "yourAppName";

    private class YourApplication implements ASAPEnvironmentChangesListener, ASAPMessageReceivedListener {
        private final ASAPPeer simplePeer;
        private Set<CharSequence> knowPeers = new HashSet<>();

        public YourApplication(ASAPPeer simplePeer) {
            // your application uses asap.
            this.simplePeer = simplePeer;

            // listen to changes in the environment
            simplePeer.addASAPEnvironmentChangesListener(this);

            // listen to received messages for your application
            simplePeer.addASAPMessageReceivedListener(YOUR_APP_NAME, this);
        }

        /**
         * Called when connection(s) changed
         * @param peerList current list of peer we have a connection to
         */
        @Override
        public void onlinePeersChanged(Set<CharSequence> peerList) {
            // peer list has changed - maybe there is a new peer around
            for(CharSequence maybeNewPeerName : peerList) {
                CharSequence newPeerName = maybeNewPeerName;
                for (CharSequence peerName : this.knowPeers) {
                    if(maybeNewPeerName.toString().equalsIgnoreCase(peerName.toString())) {
                        newPeerName = null; // not new
                        break; // found in my known peers list, try next in peerList
                    }
                }
                if(newPeerName != null) {
                    // found one - enough for this example
                    this.doSomethingWith(newPeerName); // example
                    break;
                }
            }
        }
        private void doSomethingWith(CharSequence newPeerName) {
            // create a uri
            CharSequence uri = "yourApp://" + newPeerName + "_AND_" + this.simplePeer.getPeerName() + "_haveAChat";

            try {
                // create a PDU of your applications - example
                byte[] yourMessage = this.serializeYourPDU(newPeerName);
                // send a message to any peer - recipient is in your protocol data unit
                this.simplePeer.sendASAPMessage(YOUR_APP_NAME, uri, yourMessage);
            } catch (ASAPException | IOException e) {
                System.out.println("problems: " + e.getLocalizedMessage());
            }
        }

        /**
         * Called whenever new messaged arrived
         * @param messages
         * @throws IOException
         */
        @Override
        public void asapMessagesReceived(ASAPMessages messages) throws IOException {
            Iterator<byte[]> msgIter = messages.getMessages();

            // you could check uri, e.g. to figure out what chat is addressed, what running game, what POS offering...
            CharSequence uri = messages.getURI();
            this.log("got messages ( uri | number ): (" + uri + " | " + messages.size() + ")");

            // if uri fits - you could do something with the content - your serialized data
            while(msgIter.hasNext()) {
                byte[] yourAppMessage = msgIter.next();
                this.deserialize(yourAppMessage);
            }
        }

        private byte[] serializeYourPDU(CharSequence newPeerName) throws IOException {
            // just an example
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream daos = new DataOutputStream(baos);

            // write time
            daos.writeLong(System.currentTimeMillis());
            // write local name
            daos.writeUTF(this.simplePeer.getPeerName().toString());
            // write recipient name
            daos.writeUTF(newPeerName.toString());
            daos.writeUTF("Hi there.");

            return baos.toByteArray();
        }

        private void deserialize(byte[] yourAppMessage) throws IOException {
            ByteArrayInputStream bais = new ByteArrayInputStream(yourAppMessage);
            DataInputStream dis = new DataInputStream(bais);

            // it is an example
            StringBuilder sb = new StringBuilder();
            sb.append("received message was created: " + new Date(dis.readLong()));
            sb.append("\n");

            sb.append("sender: " + dis.readUTF());
            sb.append(" | ");

            // you could decide to ignore or handle this message based on recipient
            sb.append("recipient: " + dis.readUTF());
            sb.append("\n");

            sb.append("message: " + dis.readUTF());

            this.log(sb.toString());
        }

        private void log(String msg) {
            StringBuilder sb = new StringBuilder();
            sb.append(">>>>>>>>>>>>>> YOUR APPLICATION | YOUR APPLICATION | YOUR APPLICATION <<<<<<<<<<<<<<<<<<<<<\n");
            sb.append(msg);
            sb.append("\n>>>>>>>>>>>>>> YOUR APPLICATION | YOUR APPLICATION | YOUR APPLICATION <<<<<<<<<<<<<<<<<<<<<\n");
            System.out.println(sb.toString());
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////// TEST CODE /////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * This test illustrates how a peer discovery can be used to initiate e.g. a chat a game, a POS business proposition
     * or whatever comes to your mind.
     */
    public void recognizePeerInNeighbourhood(ASAPPeer alicePeer, ASAPPeer bobPeer) {
        // have a look in the constructor or YourApplication
        YourApplication aliceInstanceOfYourApplication = new YourApplication(alicePeer);
        YourApplication bobInstanceOfYourApplication = new YourApplication(bobPeer);
    }

    @Test
    public void mockVariant() {
        // create two peer - here as peer mock.
        ASAPPeerMock alicePeer = new ASAPPeerMock(ALICE);
        ASAPPeerMock bobPeer = new ASAPPeerMock(BOB);

        this.recognizePeerInNeighbourhood(alicePeer, bobPeer);

        // simulate an encounter
        alicePeer.startEncounter(bobPeer);

        // trigger another exchange
        alicePeer.startEncounter(bobPeer);
    }

    @Test
    public void realASAPVariant() throws IOException, ASAPException, InterruptedException {
        // create two peer - here with real ASAP.
        Collection<CharSequence> formats = new ArrayList<>();
        formats.add(YOUR_APP_NAME);
        ASAPTestPeerFS alicePeer = new ASAPTestPeerFS(ALICE, formats);
        ASAPTestPeerFS bobPeer = new ASAPTestPeerFS(BOB, formats);

        this.recognizePeerInNeighbourhood(alicePeer, bobPeer);

        // trigger a real asap encounter - that's no mock, it's the real asap engine / protocol stack using TCP/IP.
        alicePeer.startEncounter(7777, bobPeer);

        Thread.sleep(1000);
    }
}
