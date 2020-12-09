package net.sharksystem.asap.listenermanager;

import net.sharksystem.asap.apps.ASAPEnvironmentChangesListener;
import net.sharksystem.asap.apps.ASAPEnvironmentChangesListenerManagement;

import java.util.List;

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

    public void notifyListeners(List<CharSequence> peerList) {
        for(ASAPEnvironmentChangesListener listener : this.listenerList) {
            listener.onlinePeersChanged(peerList);
        }
    }
}
