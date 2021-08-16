package net.sharksystem.asap.listenermanager;

import net.sharksystem.asap.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

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

    @Override
    public int getNumberListener() {
        if(this.listenerMap == null) return 0;
        return this.listenerMap.size();
    }

    public void removeAllListeners() {
        // reset
        this.listenerMap = new HashMap();
    }

    public void notifyReceived(CharSequence format, ASAPMessages asapMessage,
                               String senderE2E, // E2E part
                               List<ASAPHop> asapHopList) {

        this.notifyReceived(format, asapMessage, false, senderE2E, asapHopList);
    }

    public void notifyReceived(CharSequence format, ASAPMessages asapMessage, boolean useThreads,
                               String senderE2E, List<ASAPHop> asapHopList) {

        GenericListenerImplementation<ASAPMessageReceivedListener> listenerList = this.listenerMap.get(format);
        if(listenerList != null) {
            ASAPMessageReceivedNotifier asapMessageReceivedNotifier
                    = new ASAPMessageReceivedNotifier(asapMessage, senderE2E, asapHopList);

            listenerList.notifyAll(asapMessageReceivedNotifier, useThreads);
        }
    }

    public class ASAPMessageReceivedNotifier implements GenericNotifier<ASAPMessageReceivedListener> {
        private final ASAPMessages asapMessage;
        private final String senderE2E;
        private List<ASAPHop> asapHopList;

        ASAPMessageReceivedNotifier(ASAPMessages asapMessage,
                                    String senderE2E,
                                    List<ASAPHop> asapHopList) {

            this.asapMessage = asapMessage;
            this.senderE2E = senderE2E;
            this.asapHopList = asapHopList;
        }

        public void doNotify(ASAPMessageReceivedListener listener) {
            try {
                listener.asapMessagesReceived(this.asapMessage, this.senderE2E, this.asapHopList);
            } catch (IOException e) {
                System.err.println("error when notifying about received asap message: "
                        + e.getLocalizedMessage());
            }
        }
    }
}