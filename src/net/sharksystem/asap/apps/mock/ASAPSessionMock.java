package net.sharksystem.asap.apps.mock;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.apps.*;
import net.sharksystem.asap.listenermanager.ASAPEnvironmentChangesListenerManager;
import net.sharksystem.asap.listenermanager.ASAPMessageReceivedListenerManager;

import java.util.*;

public class ASAPSessionMock implements ASAPPeerServices {
    private boolean connected = false;

    /**
     * Simulate a connection - this mock will notify all registered listeners and remove messages.
     * After connected - any sendASAPMessage call will immediately lead to a listener call.
     */
    public void connect() {
        this.connected = true;
        this.notifyMessageReceived();
    }

    /**
     * disconnect - opposite of connect
     */
    public void disconnect() {
        this.connected = false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                      ASAPMessageManagement                                         //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
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

        if(this.connected) this.notifyMessageReceived();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                      ASAPMessageReceivedListener                                   //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    private ASAPMessageReceivedListenerManager asapMessageReceivedListenerManager =
            new ASAPMessageReceivedListenerManager();

    public void addASAPMessageReceivedListener(CharSequence format, ASAPMessageReceivedListener listener) {
        this.asapMessageReceivedListenerManager.addASAPMessageReceivedListener(format, listener);
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                      ASAPEnvironmentChangesListener                                //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    private ASAPEnvironmentChangesListenerManager environmentChangesListenerManager =
            new ASAPEnvironmentChangesListenerManager();

    @Override
    public void addASAPEnvironmentChangesListener(ASAPEnvironmentChangesListener changesListener) {
        this.environmentChangesListenerManager.addASAPEnvironmentChangesListener(changesListener);
    }

    @Override
    public void removeASAPEnvironmentChangesListener(ASAPEnvironmentChangesListener changesListener) {
        this.environmentChangesListenerManager.removeASAPEnvironmentChangesListener(changesListener);
    }
}
