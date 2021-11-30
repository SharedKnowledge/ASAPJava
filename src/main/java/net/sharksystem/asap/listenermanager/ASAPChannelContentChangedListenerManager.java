package net.sharksystem.asap.listenermanager;

import net.sharksystem.asap.*;

import java.util.HashMap;

public class ASAPChannelContentChangedListenerManager implements ASAPChannelContentChangedListenerManagement {
    private HashMap<CharSequence, GenericListenerImplementation<ASAPChannelContentChangedListener>> listenerMap =
            new HashMap();

    @Override
    public void addASAPChannelContentChangedListener(CharSequence format, ASAPChannelContentChangedListener listener) {
        GenericListenerImplementation<ASAPChannelContentChangedListener> listenerList = this.listenerMap.get(format);
        if(listenerList == null) {
            listenerList = new GenericListenerImplementation<ASAPChannelContentChangedListener>();
            this.listenerMap.put(format, listenerList);
        }

        listenerList.addListener(listener);
    }

    @Override
    public void removeASAPChannelContentChangedListener(CharSequence format, ASAPChannelContentChangedListener listener) {
        GenericListenerImplementation<ASAPChannelContentChangedListener> listenerList = this.listenerMap.get(format);
        if(listenerList != null) {
            listenerList.removeListener(listener);
        }
    }

    public int getNumberListener() {
        if(this.listenerMap == null) return 0;
        return this.listenerMap.size();
    }

    public void removeAllListeners() {
        // reset
        this.listenerMap = new HashMap();
    }

    public void notifyChanged(CharSequence format, CharSequence uri, int era) {

        this.notifyChanged(format, uri, era, false);
    }

    public void notifyChanged(CharSequence format, CharSequence uri, int era, boolean useThreads) {

        GenericListenerImplementation<ASAPChannelContentChangedListener> listenerList = this.listenerMap.get(format);
        if(listenerList != null) {
            ASAPChannelContentChangedListenerManager.ASAPChannelContentChangedNotifier notifier
                    = new ASAPChannelContentChangedListenerManager.ASAPChannelContentChangedNotifier(format, uri, era);

            listenerList.notifyAll(notifier, useThreads);
        }
    }

    public class ASAPChannelContentChangedNotifier implements GenericNotifier<ASAPChannelContentChangedListener> {
        private CharSequence format;
        private CharSequence uri;
        private int era;

        ASAPChannelContentChangedNotifier(CharSequence format, CharSequence uri, int era) {
            this.format = format;
            this.uri = uri;
            this.era = era;
        }

        public void doNotify(ASAPChannelContentChangedListener listener) {
            listener.asapChannelContentChanged(this.format, this.uri, this.era);
        }
    }
}
