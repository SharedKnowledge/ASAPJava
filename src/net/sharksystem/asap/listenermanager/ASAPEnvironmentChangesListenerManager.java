package net.sharksystem.asap.listenermanager;

import net.sharksystem.asap.apps.ASAPEnvironmentChangesListener;
import net.sharksystem.asap.apps.ASAPEnvironmentChangesListenerManagement;

import java.util.List;
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
