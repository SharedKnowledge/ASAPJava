package net.sharksystem.asap.listenermanager;

import net.sharksystem.asap.ASAPEnvironmentChangesListener;
import net.sharksystem.asap.ASAPEnvironmentChangesListenerManagement;
import net.sharksystem.utils.Log;

import java.util.Set;

public class ASAPEnvironmentChangesListenerManager
        extends GenericListenerImplementation<ASAPEnvironmentChangesListener>
        implements ASAPEnvironmentChangesListenerManagement {

    @Override
    public void addASAPEnvironmentChangesListener(ASAPEnvironmentChangesListener changesListener) {
        this.addListener(changesListener);
    }

    @Override
    public void removeASAPEnvironmentChangesListener(ASAPEnvironmentChangesListener changesListener) {
        this.removeListener(changesListener);
    }

    public void notifyListeners(Set<CharSequence> peerList) {
        if(peerList == null || peerList.isEmpty()) return;
        if(this.listenerList == null || this.listenerList.isEmpty()) return;
        for(ASAPEnvironmentChangesListener listener : this.listenerList) {
            if(listener != null) listener.onlinePeersChanged(peerList);
        }
    }
}
