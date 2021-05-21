package net.sharksystem.asap.listenermanager;

import net.sharksystem.EncounterConnectionType;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.ASAPMessageReceivedListener;
import net.sharksystem.asap.ASAPMessageReceivedListenerManagement;

import java.io.IOException;
import java.util.HashMap;

public class ASAPMessageReceivedListenerManager implements ASAPMessageReceivedListenerManagement {
    private HashMap<CharSequence, GenericListenerImplementation<ASAPMessageReceivedListener>> listenerMap =
            new HashMap();

    @Override
    public void addASAPMessageReceivedListener(CharSequence format, ASAPMessageReceivedListener listener) {
        GenericListenerImplementation<ASAPMessageReceivedListener> listenerList = this.listenerMap.get(format);
        if(listenerList == null) {
            listenerList = new GenericListenerImplementation<ASAPMessageReceivedListener>();
            this.listenerMap.put(format, listenerList);
        }

        listenerList.addListener(listener);
    }

    @Override
    public void removeASAPMessageReceivedListener(CharSequence format, ASAPMessageReceivedListener listener) {
        GenericListenerImplementation<ASAPMessageReceivedListener> listenerList = this.listenerMap.get(format);
        if(listenerList != null) {
            listenerList.removeListener(listener);
        }
    }

    public void removeAllListeners() {
        // reset
        this.listenerMap = new HashMap();
    }

    public void notifyReceived(CharSequence format, ASAPMessages asapMessage,
                               String senderE2E, // E2E part
                               String senderPoint2Point, boolean verified, boolean encrypted, // Point2Point part
                               EncounterConnectionType connectionType) {

        this.notifyReceived(format, asapMessage, false, senderE2E, senderPoint2Point,
                verified, encrypted, connectionType);
    }

    public void notifyReceived(CharSequence format, ASAPMessages asapMessage, boolean useThreads,
                               String senderE2E, String senderPoint2Point, boolean  verified, boolean encrypted,
                               EncounterConnectionType connectionType) {

        GenericListenerImplementation<ASAPMessageReceivedListener> listenerList = this.listenerMap.get(format);
        if(listenerList != null) {
            ASAPMessageReceivedNotifier asapMessageReceivedNotifier
                    = new ASAPMessageReceivedNotifier(asapMessage, senderE2E, senderPoint2Point,
                        verified, encrypted, connectionType);

            listenerList.notifyAll(asapMessageReceivedNotifier, useThreads);
        }
    }

    public class ASAPMessageReceivedNotifier implements GenericNotifier<ASAPMessageReceivedListener> {
        private final ASAPMessages asapMessage;
        private final String senderE2E;
        private final String senderPoint2Point;
        private final boolean verified;
        private final boolean encrypted;
        private final EncounterConnectionType connectionType;

        ASAPMessageReceivedNotifier(ASAPMessages asapMessage,
                                    String senderE2E,
                                    String senderPoint2Point, boolean verified, boolean encrypted,
                                    EncounterConnectionType connectionType) {

            this.asapMessage = asapMessage;
            this.senderE2E = senderE2E;
            this.senderPoint2Point = senderPoint2Point;
            this.verified = verified;
            this.encrypted = encrypted;
            this.connectionType = connectionType;
        }

        public void doNotify(ASAPMessageReceivedListener listener) {
            try {
                listener.asapMessagesReceived(this.asapMessage, this.senderE2E, this.senderPoint2Point,
                        this.verified, this.encrypted, this.connectionType);
            } catch (IOException e) {
                System.err.println("error when notifying about received asap message: "
                        + e.getLocalizedMessage());
            }
        }
    }
}