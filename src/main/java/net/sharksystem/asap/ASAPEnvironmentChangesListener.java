package net.sharksystem.asap;

import java.util.Set;

public interface ASAPEnvironmentChangesListener {
    /**
     * ASAP peers establish connections on their own and usually if possible. This
     * message is called if one or more connections could be established or got lost.
     * @param peerList current list of peer we have a connection to
     */
    void onlinePeersChanged(Set<CharSequence> peerList);
}
