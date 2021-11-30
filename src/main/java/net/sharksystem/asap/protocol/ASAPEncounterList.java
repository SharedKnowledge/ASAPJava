package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.util.Set;

public interface ASAPEncounterList {
    /**
     * Provides a set of peer id. There is record of a previous encounter with those peers.
     * @return
     */
    Set<String> getEncounteredPeers();

    /**
     * Provide era of last encounter with a peer. The local era, the counting of this peer is returned.
     * @param peerID
     * @return
     * @throws ASAPException There is not such peerID
     */
    int getLocalMostRecentEra(CharSequence peerID) throws ASAPException;

    /**
     * Provide the most era from a most recent chunk received by this peer.
     * @param peerID
     * @return
     * @throws ASAPException There is not such peerID
     */
    int getTheirMostRecentEra(CharSequence peerID) throws ASAPException, IOException;
}
