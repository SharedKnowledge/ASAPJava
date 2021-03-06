package net.sharksystem.hub;

import java.io.IOException;
import java.util.Collection;

/**
 * Hub connector is an interface that is used on peers' side to communicate with a hub. A protocol is required that
 * runs between this connector and the hub. This project provides an implementation. Use it. Spares you time
 * from implementing a trivial protocol.
 *
 * @author Thomas Schwotzer
 * @see Hub
 */
public interface HubConnector {
    /**
     * A hub can asked to provide a list of peer names which are connected right now.
     * @return peer name list - can be empty but not null.
     * @throws IOException communication problem, e.g. not connected to a hub
     */
    Collection<CharSequence> getPeerIDs() throws IOException;

    /**
     * Refresh hub information. Hub status can change. Peers can come and go. A hub is not considered to broadcast those changes. This
     * method synchronizes this side (peers' side) with the hub.
     * @throws IOException communication problem, e.g. not connected to a hub
     */
    void syncHubInformation() throws IOException;

    /**
     * Hub is asked to establish a connection to a peer. It is an asynchronous call. A listener is called
     * when a connection was established. Most probably, a new channel (e.g. TCP channel) is created.
     * @throws IOException communication problem, e.g. not connected to a hub
     */
    void connectPeer(CharSequence peerID) throws IOException;

    /**
     * Peers' side connects and registers itself with the hub.
     * There a no address information of the hub in this call. Those protocol specific information are meant
     * to be defined with the e.g. contractor of an implementation of this interface. In any case, it is assumed
     * that an instance of a hub connector is only connected with one hub.
     *
     * @param localPeerID a name under which this peer registers itself on the hub
     * @throws IOException communication problem, cannot connect to hub
     */
    void connectHub(CharSequence localPeerID) throws IOException;

    /**
     * Disconnect from hub.
     * @throws IOException communication problem, cannot connect to hub
     */
    void disconnect() throws IOException;

    /**
     * Set an object that deals with newly established connections.
     * A listener is called whenever a new connection is established between two peer mediated by the hub.
     * A connection establishment could be caused by either this local peer or another peer that found this peer
     * on the hub.
     *
     * @param listener A single listener object. It would overwrite an existing listener.
     * @see #connectPeer(CharSequence)
     */
    void setListener(NewConnectionListener listener);
}
