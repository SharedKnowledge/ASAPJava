package net.sharksystem.asap.listenermanager;

import net.sharksystem.utils.Log;

import java.util.ArrayList;
import java.util.List;

public class GenericListenerImplementation<L> {
    protected List<L> listenerList = new ArrayList<L>();

    protected void addListener(L listener) {
        if(!this.listenerList.contains(listener)) {
            this.listenerList.add(listener);
        }
    }

    protected void removeListener(L listener) {
        this.listenerList.remove(listener);
    }

    public void removeAllListeners() {
        this.listenerList = new ArrayList<L>();
    }

    public void notifyAll(GenericNotifier notifier, boolean useThreads) {
        for(L listener : this.listenerList) {
            if(useThreads) {
                new Thread (new Runnable() {
                    @Override
                    public void run() { notifier.doNotify(listener); }
                }).start();
            } else { // no threads
                notifier.doNotify(listener);
            }
        }
    }

    protected void log(String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(Log.startLog(this));
        sb.append(": ");
        sb.append(msg);

        System.out.println(sb.toString());
    }

}
