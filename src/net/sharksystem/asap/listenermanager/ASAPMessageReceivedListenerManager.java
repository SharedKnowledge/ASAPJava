package net.sharksystem.asap.listenermanager;

import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.apps.ASAPMessageReceivedListener;
import net.sharksystem.asap.apps.ASAPMessageReceivedListenerManagement;

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

    public void notifyReceived(CharSequence format, ASAPMessages asapMessage) {
        this.notifyReceived(format, asapMessage, false);
    }

    public void notifyReceived(CharSequence format, ASAPMessages asapMessage, boolean useThreads) {
        GenericListenerImplementation<ASAPMessageReceivedListener> listenerList = this.listenerMap.get(format);
        if(listenerList != null) {
            ASAPMessageReceivedNotifier asapMessageReceivedNotifier
                    = new ASAPMessageReceivedNotifier(asapMessage);

            listenerList.notifyAll(asapMessageReceivedNotifier, useThreads);
        }
    }

    public class ASAPMessageReceivedNotifier implements GenericNotifier<ASAPMessageReceivedListener> {
        private final ASAPMessages asapMessage;

        ASAPMessageReceivedNotifier(ASAPMessages asapMessage) {
            this.asapMessage = asapMessage;
        }

        public void doNotify(ASAPMessageReceivedListener listener) {
            try {
                listener.asapMessagesReceived(this.asapMessage);
            } catch (IOException e) {
                System.err.println("error when notifying about received asap message: "
                        + e.getLocalizedMessage());
            }
        }
    }
}