package net.sharksystem.streams;

import java.util.ArrayList;
import java.util.List;

public class StreamPairListenerManager {
    protected List<StreamPairListener> listenerList = new ArrayList<>();

    public void addListener(StreamPairListener listener) {
        this.listenerList.add(listener);
    }

    protected void notifyAllListenerClosed(StreamPair closedStreamPair, String key) {
        for(StreamPairListener listener : this.listenerList) {
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    listener.notifyClosed(closedStreamPair, key);
                }
            })).start();
        }
    }
}
