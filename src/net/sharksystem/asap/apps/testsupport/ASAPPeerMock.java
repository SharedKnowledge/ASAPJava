package net.sharksystem.asap.apps.testsupport;

import net.sharksystem.asap.*;
import net.sharksystem.asap.engine.ASAPChunkReceivedListener;
import net.sharksystem.asap.protocol.ASAPConnection;
import net.sharksystem.asap.utils.PeerIDHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * This class mocks an ASAPApplication. The ASAP protocol stack is bypassed completely. You can
 * test your applications logic without any fear of ASAP bugs.
 *
 */
public class ASAPPeerMock extends ASAPListenerManagingPeer implements ASAPPeerService {
    private final CharSequence peerID;

    public ASAPPeerMock(CharSequence peerID) {
        this.peerID = peerID;
    }

    public ASAPPeerMock() {
        this(ASAP.createUniqueID());
    }

    @Override
    public boolean samePeer(ASAPPeer otherPeer) {
        return this.samePeer(otherPeer.getPeerID());
    }

    public boolean samePeer(CharSequence otherPeerID) {
        return PeerIDHelper.sameID(this.getPeerID(), otherPeerID);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                      Encounter simulation                                          //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /** peer name, era of last encounter */
    private HashMap<CharSequence, Integer> encounterEra = new HashMap<>();
    private int currentEra = ASAP.INITIAL_ERA;

    private void newEra() {
        synchronized(this.appMsgStorage) {
            if(!this.appMsgStorage.isEmpty()) {
                this.log("messages were stored in this era");
                this.currentEra = ASAP.nextEra(this.currentEra);
                this.log("we enter a new era: " + this.currentEra);
                this.previousErasMsgStorage.add(this.appMsgStorage);
                this.appMsgStorage = new HashMap<>();
            } else {
                this.log("no new messages - era remains unchanged: " + this.currentEra);
            }
        }
    }

    public void startEncounter(ASAPPeerMock otherPeer) {
        this.startEncounter(otherPeer, true);
    }

    private void startEncounter(ASAPPeerMock otherPeer, boolean callOtherSide) {
        this.log("start simulation of encounter with: " + otherPeer.getPeerID());

        this.newEra();

        // already met this peer?
        Integer lastMetEra = this.encounterEra.get(otherPeer.getPeerID());
        if(lastMetEra == null) { // never met
            this.log("never met before: " + otherPeer.getPeerID());
            lastMetEra = ASAP.INITIAL_ERA;
        } else {
            this.log("last encounter: " + otherPeer.getPeerID() + " | " + lastMetEra);
        }

        // remember this encounter
        this.encounterEra.put(otherPeer.getPeerID(), this.currentEra);
        this.environmentChangesListenerManager.notifyListeners(this.encounterEra.keySet());

        // simulate message sending
        for(int tmpEra = lastMetEra; tmpEra < this.currentEra; tmpEra++) {
            otherPeer.takeMessages(this, tmpEra, this.previousErasMsgStorage.get(tmpEra));
        }

        if(callOtherSide) {
            otherPeer.startEncounter(this, false);
        }
    }

    private void takeMessages(ASAPPeerMock asapPeerMock, int tmpEra, Map<CharSequence, Map<CharSequence, List<byte[]>>> appMessages) {
        if(appMessages != null && !appMessages.isEmpty()) {
            this.log("handle received messages (sender | era): " + asapPeerMock.getPeerID() + " | " + tmpEra);
            this.notifyMessagesReceived(appMessages);
        }
    }

    private void notifyMessagesReceived(Map<CharSequence, Map<CharSequence, List<byte[]>>> appUriMessages) {
        // notify about new messages == simulate sending
        for(CharSequence appName : appUriMessages.keySet()) {
            Map<CharSequence, List<byte[]>> appMap = appUriMessages.get(appName);
            if(appMap != null) {
                Set<CharSequence> uris = appMap.keySet();
                for(CharSequence uri : uris) {
                    List<byte[]> serializedAppPDUs = appMap.get(uri);
                    ASAPMessages messagesMock = new ASAPMessagesMock(appName, uri, serializedAppPDUs);
                    this.asapMessageReceivedListenerManager.notifyReceived(appName, messagesMock, true);
                }
            }
        }
    }

    private void notifyMessageReceived() {
        Map<CharSequence, Map<CharSequence, List<byte[]>>> appUriMessages = null;
        synchronized(this.appMsgStorage) {
            if(this.appMsgStorage.isEmpty()) return; // nothing to do
            // else copy
            appUriMessages = this.appMsgStorage;
            // create empty
            this.appMsgStorage = new HashMap<>();

            // now: new message can be written and do not disturb notification process
        }

        // notify about new messages == simulate sending
        this.notifyMessagesReceived(appUriMessages);
    }

    public CharSequence getPeerID() {
        return this.peerID;
    }

    @Override
    public ASAPStorage getASAPStorage(CharSequence format) throws IOException, ASAPException {
        throw new ASAPException("Do you need this in a mock? Can't you take ASAPTestPeerFS? If not: Let me know. I can provide this method. It is not a big thing. Thx thsc42");
    }

    @Override
    public boolean isASAPRoutingAllowed(CharSequence applicationFormat) {
        return false;
    }

    @Override
    public void setASAPRoutingAllowed(CharSequence applicationFormat, boolean allowed) throws IOException, ASAPException {

    }

    @Override
    public ASAPConnection handleConnection(InputStream is, OutputStream os,
               boolean encrypt, boolean sign,
               Set<CharSequence> appsWhiteList, Set<CharSequence> appsBlackList) throws IOException, ASAPException {
        throw new ASAPException("not implemented - should it?");
    }

    @Override
    public ASAPConnection handleConnection(InputStream is, OutputStream os) throws IOException, ASAPException {
        throw new ASAPException("that's a mock, no real peer");
    }

    @Override
    public void overwriteChuckReceivedListener(ASAPChunkReceivedListener listener) {
        throw new RuntimeException("that's a mock, no real peer");
    }

    @Override
    public void sendOnlineASAPMessage(CharSequence appName, CharSequence uri, byte[] message) throws ASAPException, IOException {
        throw new RuntimeException("that's a mock, no real peer");
    }

    public void stopEncounter(ASAPPeerMock otherPeer) {
        this.stopEncounter(otherPeer, true);
    }

    public void stopEncounter(ASAPPeerMock otherPeer, boolean callOtherSide) {
        this.log("stop encounter with " + otherPeer.getPeerID());
        this.newEra();
        if(callOtherSide) {
            otherPeer.stopEncounter(this, false);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                      ASAPMessageManagement                                         //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /** appName, <uri, messageBytes> */
    private List< Map<CharSequence, Map<CharSequence, List<byte[]> > > > previousErasMsgStorage = new ArrayList<>();

    /** appName, <uri, messageBytes> */
    private Map<CharSequence, Map<CharSequence, List<byte[]>>> appMsgStorage = new HashMap<>();

    private List<byte[]> getStorage(CharSequence appName, CharSequence uri) {
        Map<CharSequence, List<byte[]>> charSequenceListMap = this.appMsgStorage.get(appName);
        if (charSequenceListMap == null) {
            charSequenceListMap = new HashMap<>();
            this.appMsgStorage.put(appName, charSequenceListMap);
        }

        List<byte[]> byteMessageList = charSequenceListMap.get(uri);
        if (byteMessageList == null) {
            byteMessageList = new ArrayList<>();
            charSequenceListMap.put(uri, byteMessageList);
        }

        return byteMessageList;
    }

    @Override
    public void sendASAPMessage(CharSequence appName, CharSequence uri, byte[] message) throws ASAPException {
        synchronized(this.appMsgStorage) {
            List<byte[]> storage = this.getStorage(appName, uri);
            storage.add(message);
        }
    }
}
