package net.sharksystem.asap;

import java.util.Set;

public interface ASAPConnectionChangedListener {
    void asapConnectionedPeers(Set<CharSequence> listOfPeerIDs);
}
