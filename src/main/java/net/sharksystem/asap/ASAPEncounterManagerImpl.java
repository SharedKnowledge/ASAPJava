package net.sharksystem.asap;

import com.sun.istack.NotNull;
import net.sharksystem.asap.protocol.ASAPConnection;
import net.sharksystem.asap.protocol.ASAPConnectionListener;
import net.sharksystem.asap.utils.PeerIDHelper;
import net.sharksystem.utils.streams.StreamPair;
import net.sharksystem.utils.Log;

import java.io.*;
import java.util.*;

public class ASAPEncounterManagerImpl implements ASAPEncounterManager, ASAPConnectionListener {
    public static final long DEFAULT_WAIT_BEFORE_RECONNECT_TIME = 1000; // a second - debugging
    public static final long DEFAULT_WAIT_TO_AVOID_RACE_CONDITION = 500; // milliseconds - worked fine with BT.

    private final int randomValue;
    private final long waitBeforeReconnect;
    private ASAPConnectionHandler asapConnectionHandler; // object that will eventually run the ASAP session

    /*
    PeerID --[peerRemoteAddress]--(n)--> remoteAddress<Set>
    remoteAddressORPeerID --[openStreamPairs]--(1)--> StreamPair
    remoteAddressORPeerID --[encounterDate]--(1)--> Date
    ASAPConnection --[openASAPConnections]--(1)--> remoteAddressORPeerID
     */

    /**
     * We keep all open stream pairs indexed by their remote address to avoid identical connections and 'flickering'
     * remote address -> stream pair
     */
    private Map<CharSequence, StreamPair> openStreamPairs = new HashMap<>();

    /** open ASAP connections with their stream pair: asap connection -> remote address */
    private Map<ASAPConnection, CharSequence> openASAPConnections = new HashMap<>();

    /** remember last encounter: remote address -> date */
    private Map<CharSequence, Date> encounterDate = new HashMap<>();

    /** remember remote address of peers (they can have more than one): peerID -> remote address */
    private Map<CharSequence, Set<CharSequence>> peerRemoteAddresses = new HashMap<>();

    public ASAPEncounterManagerImpl(@NotNull ASAPConnectionHandler asapConnectionHandler) {
        this(asapConnectionHandler, DEFAULT_WAIT_BEFORE_RECONNECT_TIME);
    }

    public ASAPEncounterManagerImpl(@NotNull ASAPConnectionHandler asapConnectionHandler, long waitingPeriod) {
        this.asapConnectionHandler = asapConnectionHandler;
        this.randomValue = new Random(System.currentTimeMillis()).nextInt();
        this.waitBeforeReconnect = waitingPeriod;
    }

    private boolean coolDownOver(CharSequence id, EncounterConnectionType connectionType) {
        Date now = new Date();
        Date lastEncounter = this.encounterDate.get(id);

        if(lastEncounter == null) {
            Log.writeLog(this, this.toString(), "device/peer not in encounteredDevices - should connect");
            this.encounterDate.put(id, now);
            return true;
        }

        // calculate reconnection time

        // get current time, in its incarnation as date
        long nowInMillis = System.currentTimeMillis();
        long reconnectedBeforeInMillis = nowInMillis - this.waitBeforeReconnect;
        Date reconnectBefore = new Date(reconnectedBeforeInMillis);

        Log.writeLog(this, this.toString(), "now: " + now.toString());
        Log.writeLog(this, this.toString(), "connectBefore: " + reconnectBefore.toString());

        // known peer
        Log.writeLog(this, this.toString(), "device/peer (" + id + ") in encounteredDevices list?");
        // it was in the list
        if(lastEncounter.before(reconnectBefore)) {
            Log.writeLog(this, this.toString(),  "yes - should connect: " + id);
            // remember that and overwrite previous entry
            this.encounterDate.put(id, now);
            return true;
        }

        Log.writeLog(this, this.toString(), "should not connect - recently met: " + id);
        return false;
    }

    @Override
    public boolean shouldCreateConnectionToPeer(CharSequence remoteAdressOrPeerID, EncounterConnectionType connectionType) {
        // do we have a connection under a peerID?
        StreamPair streamPair = this.openStreamPairs.get(remoteAdressOrPeerID);
        if(streamPair != null) {
            return false;
        }

        // we know this peer and we are still in cool down period
        if(!this.coolDownOver(remoteAdressOrPeerID, connectionType)) return false;

        // is this id a remote adress?
        Set<CharSequence> remoteAddresses = this.peerRemoteAddresses.get(remoteAdressOrPeerID);
        if(remoteAddresses == null) {
            // we do not know this peer with no address - connect
            return true;
        }

        // is there an open connection with any of those addresses?
        for(CharSequence remoteAddress : remoteAddresses) {
            streamPair = this.openStreamPairs.get(remoteAddress);
            if(streamPair != null) {
                // if there is a single reason not to connect - we do not connect
                if(!this.coolDownOver(remoteAddress, connectionType)) return false;
            }
        }

        // no open connection and cool down period over for any known address - connect
        return true;
    }

    @Override
    public void handleEncounter(StreamPair streamPair, EncounterConnectionType connectionType) throws IOException {
        this.handleEncounter(streamPair, connectionType, false, false);
    }

    @Override
    public void handleEncounter(StreamPair streamPair, EncounterConnectionType connectionType, boolean initiator)
            throws IOException {

        this.handleEncounter(streamPair, connectionType, initiator, true);
    }

    private void handleEncounter(StreamPair streamPair, EncounterConnectionType connectionType, boolean initiator,
                                boolean raceCondition) throws IOException {

        CharSequence streamPairID = streamPair.getSessionID();

        Log.writeLog(this, this.toString(), "socket called: handle new encounter" + streamPair);

        // should we connect in the first place
        if (!this.shouldCreateConnectionToPeer(streamPairID, connectionType)) {
            // no - than shut it down.
            streamPair.close();
            return;
        }

        // new stream pair is ok. Is there a race condition expected ?
        if(raceCondition) {
            // avoid the nasty race condition
            boolean waited = this.waitBeforeASAPSessionLaunch(
                    streamPair.getInputStream(),
                    streamPair.getOutputStream(),
                    initiator, DEFAULT_WAIT_TO_AVOID_RACE_CONDITION);

            // ask again?
            if (waited) {
                if (!this.shouldCreateConnectionToPeer(streamPairID, connectionType)) {
                    streamPair.close();
                    return;
                }
            }
        }

        // we a through with it - remember that new stream pair
        Log.writeLog(this, this.toString(), "remember streamPair: " + streamPair);
        this.openStreamPairs.put(streamPairID, streamPair);

        Log.writeLog(this, this.toString(), "going to launch a new asap connection");

        try {
            Log.writeLog(this, this.toString(), "call asap peer to handle connection");
            ASAPConnection asapConnection =
                    this.asapConnectionHandler.handleConnection(
                            streamPair.getInputStream(), streamPair.getOutputStream(), connectionType);

            asapConnection.addASAPConnectionListener(this);

            this.openASAPConnections.put(asapConnection, streamPairID);

        } catch (IOException | ASAPException e) {
            Log.writeLog(this, this.toString(), "while launching asap connection: "
                    + e.getLocalizedMessage());
        }
    }

    private boolean waitBeforeASAPSessionLaunch(InputStream is, OutputStream os, boolean connectionInitiator,
                           long waitInMillis) throws IOException {
        // run a little negotiation before we start
        DataOutputStream dos = new DataOutputStream(os);
        int remoteValue = 0;

        try {
            dos.writeInt(this.randomValue);
            DataInputStream dis = new DataInputStream(is);
            remoteValue = dis.readInt();
        } catch (IOException e) {
            // decision is made - this connection is dead
            os.close();
            is.close();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("try to solve race condition: localValue == ");
        sb.append(this.randomValue);
        sb.append(" | remoteValue == ");
        sb.append(remoteValue);
        sb.append(" | initiator == ");
        sb.append(connectionInitiator);

        int initiatorValue, nonInitiatorValue;
        if(connectionInitiator) {
            initiatorValue = this.randomValue;
            nonInitiatorValue = remoteValue;
        } else {
            initiatorValue = remoteValue;
            nonInitiatorValue = this.randomValue;
        }

        sb.append(" | initiatorValue == ");
        sb.append(initiatorValue);
        sb.append(" | nonInitiatorValue == ");
        sb.append(nonInitiatorValue);
        Log.writeLog(this, this.toString(), sb.toString());

        /* Here comes the bias: An initiator with a smaller value waits a moment */
        if(connectionInitiator && initiatorValue < nonInitiatorValue) {
            try {
                sb = new StringBuilder();
                sb.append("wait ");
                sb.append(waitInMillis);
                sb.append(" ms");
                Log.writeLog(this, this.toString(), sb.toString());
                Thread.sleep(waitInMillis);
                return true; // waited
            } catch (InterruptedException e) {
                Log.writeLog(this, this.toString(), "wait interrupted");
            }
        }

        return false;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                           ASAPConnectionListener                                     //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public synchronized void asapConnectionStarted(String remotePeerName, ASAPConnection connection) {
        CharSequence peerID = connection.getEncounteredPeer();
        Log.writeLog(this, this.toString(), "new ASAP session started with peerID " + peerID);

        CharSequence streamPairID = this.openASAPConnections.get(connection);
        if(PeerIDHelper.sameID(streamPairID, peerID)) {
            // nothing to do. This connection was already established with peer id
            return;
        }

        // connection was established with remote address. Remember that
        Set<CharSequence> remoteAddresses = this.peerRemoteAddresses.get(peerID);
        if(remoteAddresses == null) {
            remoteAddresses = new HashSet<>();
            this.peerRemoteAddresses.put(peerID, remoteAddresses);
        }

        remoteAddresses.add(streamPairID);

    }

    @Override
    public synchronized void asapConnectionTerminated(Exception terminatingException, ASAPConnection connection) {
        CharSequence peerID = connection.getEncounteredPeer();

        CharSequence peerIDOrAddress = this.openASAPConnections.get(connection);
        this.openASAPConnections.remove(connection);

        // one call will fail - who cares.
        this.openStreamPairs.remove(peerIDOrAddress);
        this.openStreamPairs.remove(peerID);

        // was this stream pair registered with remote address? Clean peerRemoteAddress data set.
        Set<CharSequence> remoteAddresses = this.peerRemoteAddresses.get(peerID);
        if(remoteAddresses != null) {
            // this connection was registered under its remote address
            for(CharSequence remoteAddressObject : remoteAddresses) {
                if(PeerIDHelper.sameID(remoteAddressObject, peerIDOrAddress)) {
                    // found it
                    if(remoteAddresses.size() == 1) {
                        // remove whole entry - this was its only entry
                        this.peerRemoteAddresses.remove(peerID);
                    } else {
                        remoteAddresses.remove(remoteAddressObject);
                    }
                    // in any case - we are ready here
                    break;
                }
            }
        }
    }

    public String toString() {
        return this.asapConnectionHandler.toString();
    }
}
