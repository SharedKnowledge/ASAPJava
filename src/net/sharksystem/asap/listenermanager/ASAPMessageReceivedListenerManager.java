package net.sharksystem.asap.listenermanager;

import net.sharksystem.asap.*;

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
                               ASAPHop asapHop) {

        this.notifyReceived(format, asapMessage, false, senderE2E, asapHop);
    }

    public void notifyReceived(CharSequence format, ASAPMessages asapMessage, boolean useThreads,
                               String senderE2E, ASAPHop asapHop) {

        GenericListenerImplementation<ASAPMessageReceivedListener> listenerList = this.listenerMap.get(format);
        if(listenerList != null) {
            ASAPMessageReceivedNotifier asapMessageReceivedNotifier
                    = new ASAPMessageReceivedNotifier(asapMessage, senderE2E, asapHop);

            listenerList.notifyAll(asapMessageReceivedNotifier, useThreads);
        }
    }

    public class ASAPMessageReceivedNotifier implements GenericNotifier<ASAPMessageReceivedListener> {
        private final ASAPMessages asapMessage;
        private final String senderE2E;
        private ASAPHop asapHop;

        ASAPMessageReceivedNotifier(ASAPMessages asapMessage,
                                    String senderE2E,
                                    ASAPHop asapHop) {

            this.asapMessage = asapMessage;
            this.senderE2E = senderE2E;
            this.asapHop = asapHop;
        }

        public void doNotify(ASAPMessageReceivedListener listener) {
            try {
                listener.asapMessagesReceived(this.asapMessage, this.senderE2E, this.asapHop);
            } catch (IOException e) {
                System.err.println("error when notifying about received asap message: "
                        + e.getLocalizedMessage());
            }
        }
    }
}