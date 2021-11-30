package net.sharksystem.asap;

import net.sharksystem.asap.listenermanager.ASAPChannelContentChangedListenerManager;
import net.sharksystem.asap.listenermanager.ASAPEnvironmentChangesListenerManager;
import net.sharksystem.asap.listenermanager.ASAPMessageReceivedListenerManager;

public abstract class ASAPListenerManagingPeer implements ASAPPeer {
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
    //                                      ASAPChannelContentChangedListener                             //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected ASAPChannelContentChangedListenerManager asapChannelContentChangedListenerManager =
            new ASAPChannelContentChangedListenerManager();

    @Override
    public void addASAPChannelContentChangedListener(CharSequence format, ASAPChannelContentChangedListener listener) {
        this.asapChannelContentChangedListenerManager.addASAPChannelContentChangedListener(format, listener);
    }

    @Override
    public void removeASAPChannelContentChangedListener(CharSequence format, ASAPChannelContentChangedListener listener) {
        this.asapChannelContentChangedListenerManager.removeASAPChannelContentChangedListener(format, listener);
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
        sb.append(this.getPeerID());
        sb.append(": ");
        sb.append(msg);

        System.out.println(sb.toString());
    }
}
