package net.sharksystem.asap.apps.mock;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.apps.ASAPMessageReceivedListener;
import net.sharksystem.asap.apps.ASAPMessageSender;

import java.io.IOException;
import java.util.*;

public class ASAPSessionMock implements ASAPMessageSender {
    private Map<CharSequence, Map<CharSequence, List<byte[]>>> appMsgStorage = new HashMap<>();
    private Map<CharSequence, List<ASAPMessageReceivedListener>> listenerMap = new HashMap<>();
    private boolean connected = false;

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

        if(this.connected) this.notifyListeners();
    }

    public void addASAPMessageReceivedListener(CharSequence format, ASAPMessageReceivedListener listener) {
        List<ASAPMessageReceivedListener> asapMessageReceivedListeners = this.listenerMap.get(format);
        if(asapMessageReceivedListeners == null) {
            asapMessageReceivedListeners = new ArrayList<>();
            this.listenerMap.put(format, asapMessageReceivedListeners);
        }

        asapMessageReceivedListeners.add(listener);
    }

    /**
     * Simulate a connection - this mock will notify all registered listeners and remove messages.
     * After connected - any sendASAPMessage call will immediately lead to a listener call.
     */
    public void connect() {
        this.connected = true;
        this.notifyListeners();
    }

    private void notifyListeners() {
        Map<CharSequence, Map<CharSequence, List<byte[]>>> appUriMessages = null;
        synchronized(this.appMsgStorage) {
            if(this.appMsgStorage.isEmpty()) return;

            // else copy
            appUriMessages = this.appMsgStorage;

            // create empty
            this.appMsgStorage = new HashMap<>();
        }

        // send
        for(CharSequence appName : appUriMessages.keySet()) {
            Map<CharSequence, List<byte[]>> appMap = appUriMessages.get(appName);
            if(appMap != null) {
                List<ASAPMessageReceivedListener> asapMessageReceivedListeners = this.listenerMap.get(appName);
                if(asapMessageReceivedListeners != null && !asapMessageReceivedListeners.isEmpty()) {
                    Set<CharSequence> uris = appMap.keySet();
                    for(CharSequence uri : uris) {
                        List<byte[]> serializedAppPDUs = appMap.get(uri);
                        for(ASAPMessageReceivedListener listener : asapMessageReceivedListeners) {
                            // create a thread for each listener
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    ASAPMessages messagesMock = new ASAPMessagesMock(appName, uri, serializedAppPDUs);
                                    try {
                                        listener.asapMessagesReceived(messagesMock);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        }
                    }
                }
            }
        }
    }

    /**
     * disconnect - opposite of connect
     */
    public void disconnect() {
        this.connected = false;
    }
}
