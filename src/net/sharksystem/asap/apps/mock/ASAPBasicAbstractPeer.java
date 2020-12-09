package net.sharksystem.asap.apps.mock;

import net.sharksystem.asap.apps.ASAPEnvironmentChangesListener;
import net.sharksystem.asap.apps.ASAPMessageReceivedListener;
import net.sharksystem.asap.apps.ASAPPeerServices;
import net.sharksystem.asap.listenermanager.ASAPEnvironmentChangesListenerManager;
import net.sharksystem.asap.listenermanager.ASAPMessageReceivedListenerManager;

public abstract class ASAPBasicAbstractPeer implements ASAPPeerServices {
    protected final CharSequence peerName;

    protected ASAPBasicAbstractPeer(CharSequence peerName) {
        this.peerName = peerName;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                      ASAPMessageReceivedListener                                   //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected ASAPMessageReceivedListenerManager asapMessageReceivedListenerManager =
            new ASAPMessageReceivedListenerManager();

    public void addASAPMessageReceivedListener(CharSequence format, ASAPMessageReceivedListener listener) {
        this.asapMessageReceivedListenerManager.addASAPMessageReceivedListener(format, listener);
    }

    @Override
    public void removeASAPMessageReceivedListener(CharSequence format, ASAPMessageReceivedListener listener) {
        this.asapMessageReceivedListenerManager.removeASAPMessageReceivedListener(format, listener);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                      ASAPEnvironmentChangesListener                                //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected ASAPEnvironmentChangesListenerManager environmentChangesListenerManager =
            new ASAPEnvironmentChangesListenerManager();

    @Override
    public void addASAPEnvironmentChangesListener(ASAPEnvironmentChangesListener changesListener) {
        this.environmentChangesListenerManager.addASAPEnvironmentChangesListener(changesListener);
    }

    @Override
    public void removeASAPEnvironmentChangesListener(ASAPEnvironmentChangesListener changesListener) {
        this.environmentChangesListenerManager.removeASAPEnvironmentChangesListener(changesListener);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                             util                                                   //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void log(String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.peerName);
        sb.append(": ");
        sb.append(msg);

        System.out.println(sb.toString());
    }
}
