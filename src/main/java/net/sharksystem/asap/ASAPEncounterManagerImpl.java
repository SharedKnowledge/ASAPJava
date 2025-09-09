package net.sharksystem.asap;

import net.sharksystem.SharkException;
import net.sharksystem.asap.utils.DateTimeHelper;
import net.sharksystem.fs.ExtraDataFS;
import net.sharksystem.asap.protocol.ASAPConnection;
import net.sharksystem.asap.protocol.ASAPConnectionListener;
import net.sharksystem.asap.utils.ASAPSerialization;
import net.sharksystem.asap.utils.PeerIDHelper;
import net.sharksystem.utils.streams.StreamPair;
import net.sharksystem.utils.Log;

import java.io.*;
import java.util.*;

public class ASAPEncounterManagerImpl implements
        ASAPEncounterManager, ASAPEncounterManagerAdmin, ASAPConnectionListener {
    public static final long DEFAULT_WAIT_BEFORE_RECONNECT_TIME = 60000; // 60 seconds == 1 minute
    public static final long DEFAULT_WAIT_TO_AVOID_RACE_CONDITION = 500; // milliseconds - worked fine with BT.
    public static final String DATASTORAGE_FILE_EXTENSION = "em";
    private static final CharSequence ENCOUNTER_MANAGER_DENY_LIST_KEY = "denylist";
    private final CharSequence peerID;
    private ExtraDataFS extraDataStorage = null;

    private int randomValue;
    private long waitBeforeReconnect;
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

    public ASAPEncounterManagerImpl(ASAPConnectionHandler asapConnectionHandler, CharSequence peerID)
            throws SharkException, IOException {
        this(asapConnectionHandler, peerID, DEFAULT_WAIT_BEFORE_RECONNECT_TIME);
    }

    public ASAPEncounterManagerImpl(ASAPConnectionHandler asapConnectionHandler, CharSequence peerID,
                                    long waitingPeriod, CharSequence rootFolder) throws SharkException, IOException {
        this.asapConnectionHandler = asapConnectionHandler;
        this.peerID = peerID;
        this.randomValue = new Random(System.currentTimeMillis()).nextInt();
        this.waitBeforeReconnect = waitingPeriod;

        if(rootFolder != null && rootFolder.length() > 0) {
            // create folder
            this.extraDataStorage = new ExtraDataFS(rootFolder + "/" + ENCOUNTER_MANAGER_DENY_LIST_KEY, DATASTORAGE_FILE_EXTENSION);
            this.restoreDenyList();
        }
    }

    public ASAPEncounterManagerImpl(ASAPConnectionHandler asapConnectionHandler, CharSequence peerID,
                                    long waitingPeriod) throws SharkException, IOException {
        this(asapConnectionHandler, peerID, waitingPeriod, null);
    }

    private boolean coolDownOver(CharSequence id, ASAPEncounterConnectionType connectionType) {
        Date now = new Date();
        Date lastEncounter = this.encounterDate.get(id);

        if(lastEncounter == null) {
            Log.writeLog(this, this.toString(),
                    "device/peer not in encounteredDevices - add "
                            + DateTimeHelper.long2ExactTimeString(now.getTime()));
            // this.encounterDate.put(id, now); do not enter it into that list before (!!) a connection is established
            return true;
        }

        // calculate reconnection time
        Log.writeLog(this, this.toString(), "this.waitBeforeReconnect == " + this.waitBeforeReconnect);

        // get current time, in its incarnation as date
        long nowInMillis = now.getTime();
        long shouldReconnectIfBeforeInMillis = nowInMillis - this.waitBeforeReconnect;
        Date shouldReconnectIfBefore = new Date(shouldReconnectIfBeforeInMillis);

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(DateTimeHelper.long2ExactTimeString(nowInMillis));
        sb.append(" == now\n");
        sb.append(DateTimeHelper.long2ExactTimeString(shouldReconnectIfBeforeInMillis));
        sb.append(" == should reconnect if met before that moment\n");
        sb.append(DateTimeHelper.long2ExactTimeString(lastEncounter.getTime()));
        sb.append(" == last encounter");
        Log.writeLog(this, this.toString(), sb.toString());

        // known peer
        Log.writeLog(this, this.toString(), "device/peer (" + id + ") in encounteredDevices list?");
        // it was in the list
        if(lastEncounter.before(shouldReconnectIfBefore)) {
            Log.writeLog(this, this.toString(),  "yes - should connect: " + id);
            // remember that and overwrite previous entry
            /* that was a nasty bug since this method is called before establishing a connection
            * AND after connection establishment and before lauching an ASAP session
            */
            // this.encounterDate.put(id, now);
            return true;
        }

        Log.writeLog(this, this.toString(), "should not connect - recently met: " + id);
        return false;
    }

    @Override
    public boolean shouldCreateConnectionToPeer(CharSequence remoteAdressOrPeerID,
                                                ASAPEncounterConnectionType connectionType) {
        Log.writeLog(this, this.toString(), "should connect to " + remoteAdressOrPeerID + " ?");
        // on deny list?
        if(this.denyList.contains(remoteAdressOrPeerID)) return false;
        Log.writeLog(this, this.toString(), remoteAdressOrPeerID + " not on deny list");

        // do we have a connection under a peerID?
        StreamPair streamPair = this.openStreamPairs.get(remoteAdressOrPeerID);
        if(streamPair != null) {
            return false;
        } else {
            Log.writeLog(this, this.toString(), remoteAdressOrPeerID + " no parallel open connection");
        }

        // we know this peer and it is still in cool down period
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
    public void forgetPreviousEncounter() {
        this.encounterDate = new HashMap<>();
    }

    @Override
    public void handleEncounter(StreamPair streamPair, ASAPEncounterConnectionType connectionType) throws IOException {
        this.handleEncounter(streamPair, connectionType, false, false);
    }

    @Override
    public void handleEncounter(StreamPair streamPair, ASAPEncounterConnectionType connectionType, boolean initiator)
            throws IOException {

        this.handleEncounter(streamPair, connectionType, initiator, true);
    }

    @Override
    public Map<CharSequence, Date> getEncounterTime() {
        return this.encounterDate;
    }

    @Override
    public long getTimeBeforeReconnect() {
        return this.waitBeforeReconnect;
    }

    private void handleEncounter(StreamPair streamPair, ASAPEncounterConnectionType connectionType, boolean initiator,
                                 boolean raceCondition) throws IOException {
        // always exchange peerIDs
        DataOutputStream dos = new DataOutputStream(streamPair.getOutputStream());
        dos.writeUTF(this.peerID.toString());
        DataInputStream dis = new DataInputStream(streamPair.getInputStream());
        String remotePeerID = dis.readUTF();

        if(remotePeerID != null && remotePeerID.length() > 0) {
            streamPair.setEndpointID(remotePeerID);
        }

        CharSequence connectionID = streamPair.getEndpointID();
        if(connectionID == null || connectionID.length() == 0) connectionID = streamPair.getSessionID();

        Log.writeLog(this, this.toString(), "decide whether to pursue this new encounter: " + streamPair);

        // should we connect in the first place
        if (!this.shouldCreateConnectionToPeer(connectionID, connectionType)) {
            // no - than shut it down.
            Log.writeLog(this, this.toString(),
                    "close connection (on deny list or in cool down)");
            streamPair.close();
            return;
        }

        // new stream pair is ok. Is there a race condition expected ?
        if(raceCondition) {
            // avoid the nasty race condition
            Log.writeLog(this, this.toString(), "solve race condition");
            boolean waited = this.solveRaceCondition(streamPair, initiator, DEFAULT_WAIT_TO_AVOID_RACE_CONDITION);

            // ask again?
            if (waited) {
                if (!this.shouldCreateConnectionToPeer(connectionID, connectionType)) {
                    streamPair.close();
                    return;
                }
            }
        }

        // we are through with it - remember that new stream pair
        Log.writeLog(this, this.toString(), "remember streamPair: " + streamPair);
        this.openStreamPairs.put(connectionID, streamPair);
        Log.writeLog(this, this.toString(), "remember encounter: " + streamPair.getEndpointID());
        this.encounterDate.put(streamPair.getEndpointID(), new Date());
        try {
            Log.writeLog(this, this.toString(), "call asap peer to handle connection");
            ASAPConnection asapConnection =
                    this.asapConnectionHandler.handleConnection(
                            streamPair.getInputStream(), streamPair.getOutputStream(), connectionType);

            asapConnection.addASAPConnectionListener(this);

            Log.writeLog(this, this.toString(),
                    "asap peers is handling session: " + asapConnection.toString());
            this.openASAPConnections.put(asapConnection, connectionID);

        } catch (IOException | ASAPException e) {
            Log.writeLog(this, this.toString(), "while launching asap connection: "
                    + e.getLocalizedMessage());
        }
    }

    private boolean solveRaceCondition(StreamPair streamPair, boolean connectionInitiator,
                                       long waitInMillis) throws IOException {
        // run a little negotiation before we start
        DataOutputStream dos = new DataOutputStream(streamPair.getOutputStream());
        int remoteValue = 0;
        String remotePeerID = null;

        try {
            // write protocol unit
            dos.writeInt(this.randomValue);
            dos.writeUTF(this.peerID.toString());

            // read it
            DataInputStream dis = new DataInputStream(streamPair.getInputStream());
            remoteValue = dis.readInt();
            remotePeerID = dis.readUTF();

            if(remotePeerID != null && remotePeerID.length() > 0) streamPair.setEndpointID(remotePeerID);
        } catch (IOException e) {
            // decision is made - this connection is dead
            streamPair.close();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("try to solve race condition: random (local/remote) == ");
        sb.append(this.randomValue);
        sb.append("/");
        sb.append(remoteValue);
        sb.append(" | peerID == ");
        sb.append(this.peerID);
        sb.append("/");
        sb.append(remotePeerID);
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
        Log.writeLog(this, this.toString(), "new ASAP encounter: " + connection);

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
        Log.writeLog(this, this.toString(), "encounter terminated: " + connection);
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                          EncounterManagerAdmin                                             //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Set<CharSequence> denyList = new HashSet<>();
    
    //// housekeeping deny list
    public void clearDenyList() {
        this.denyList = new HashSet<>();
        try {
            this.saveDenyList();
        } catch (IOException | SharkException e) {
            Log.writeLogErr(this, this.toString(), "cannot persist deny list: " + e.getLocalizedMessage());
        }
    }
    private void restoreDenyList() throws SharkException, IOException {
        if(this.extraDataStorage == null) {
            Log.writeLog(this, this.toString(), "no persistent storage for deny list");
        } else {
            byte[] denyListBytes = this.extraDataStorage.getExtra(ENCOUNTER_MANAGER_DENY_LIST_KEY);
            if(denyListBytes != null && denyListBytes.length > 0) {
                ByteArrayInputStream bais = new ByteArrayInputStream(denyListBytes);
                this.denyList = ASAPSerialization.readCharSequenceSetParameter(bais);
            } else {
                this.clearDenyList();
            }
        }
    }

    private void saveDenyList() throws IOException, SharkException {
        if(this.extraDataStorage == null) {
            Log.writeLog(this, this.toString(), "no persistent storage for deny list");
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ASAPSerialization.writeCharSequenceSetParameter(this.denyList, baos);
            this.extraDataStorage.putExtra(ENCOUNTER_MANAGER_DENY_LIST_KEY, baos.toByteArray());
        }
    }

    ///// manage deny list
    @Override
    public void addToDenyList(CharSequence peerID) {
        this.denyList.add(peerID);
        try {
            this.saveDenyList();
        } catch (IOException | SharkException e) {
            Log.writeLogErr(this, this.toString(), "cannot persist deny list: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void removeFromDenyList(CharSequence peerID) {
        this.denyList.remove(peerID);
        try {
            this.saveDenyList();
        } catch (IOException | SharkException e) {
            Log.writeLogErr(this, this.toString(), "cannot persist deny list: " + e.getLocalizedMessage());
        }
    }

    @Override
    public Set<CharSequence> getDenyList() {
        return this.denyList;
    }

    @Override
    public Set<CharSequence> getConnectedPeerIDs() {
        return this.openStreamPairs.keySet();
    }

    public ASAPEncounterConnectionType getConnectionType(CharSequence peerID) throws ASAPException {
        for(ASAPConnection connection : this.openASAPConnections.keySet()) {
            if(PeerIDHelper.sameID(connection.getEncounteredPeer(), peerID)) {
                // found our connection
                return connection.getASAPEncounterConnectionType();
            }
        }
        throw new ASAPException("there is no connection to peer " + peerID);
    }

    @Override
    public void closeEncounter(CharSequence peerID) {
        StreamPair stream2Close = this.openStreamPairs.get(peerID);
        if(stream2Close != null) {
            stream2Close.close();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                 utils                                                      //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String toString() {
        if(this.asapConnectionHandler != null) return this.asapConnectionHandler.toString();
        else return "null";
    }

}
