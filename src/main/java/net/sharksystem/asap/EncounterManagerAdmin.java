package net.sharksystem.asap;

import java.util.Set;

/**
 * That's the admin interface of an encounter manager. It allows <b>deny list management</b>.
 * A deny list contains peerIDs. The encounter manager must not establish new connections to
 * those peers. There can be several reasons (security (peer is not considered trustworthy),
 * network topology (see considerations of a large scale ad-hoc network with limited
 * connections on each peer).
 *
 * Future enhancements:
 * <ul><li>define a point to point encryption policy.</li>
 * <li>define what connections types are acceptable (Internet, Hub, Ad-hoc, Onion)</li></ul>
 */
public interface EncounterManagerAdmin {
    /**
     * @return set of ID to which an open connection exists right now.
     */
    public abstract Set<CharSequence> getConnectedPeerIDs();

    /**
     * Add a peerID to what no connection should be established.
     * Note: Adding a peer to the deny list does not necessarily terminate an
     * existing connection to that peer.
     * @param peerID
     */
    public abstract void addToDenyList(CharSequence peerID);

    /**
     * Remove a peerID from deny list
     * @param peerID
     */
    public abstract void removeFromDenyList(CharSequence peerID);

    /**
     * Get PeerID set to which no connection should be established
     * @return
     */
    public abstract Set<CharSequence> getDenyList();

    /**
     * Cancel a connection to a peer. This method call does not change the deny list.
     * @param peerID
     */
    public abstract void cancelConnection(CharSequence peerID);

}
