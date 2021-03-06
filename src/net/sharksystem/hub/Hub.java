package net.sharksystem.hub;

import net.sharksystem.asap.ASAPPeer;

/**
 * A hub is an infrastructure element supporting ASAP applications. Any ASAP communication takes place during
 * a peer encounter. In technical terms: An encounter is performed on a stream based point-to-point connection.
 * An encounter is actually the exchange of a series of ASAP PDUs.
 * <br/><br/>
 * That point-to-point connection is platform specific. Ad-hoc networks, e.g. based on Bluetooth or Wifi-direct
 * are a good choice. Using Internet is always a good idea ;) But here is a conceptual challenge:
 * <br/><br/>
 * ASAP apps are meant to be pure decentralized with a strong emphasis on avoiding any entity that even remotely smells
 * like a server. It is all about data. Who can see data? Who gets even wind of the fact that two peer communicate.
 * Have a look at the ASAP design principles regarding that topic and especially metadata.
 * <br/><br/>
 * Nevertheless, peers must be able to communicate over greater distances. And yes, even we decentralized enthusiasts
 * realize that multihop ad-hoc networks are often only a fallback and not always the best choice. How to deal with it?
 * There are some (hopefully good) answers. Hub is one proposal. Repeater another one.
 * <br/><br/>
 * A hub is considered to be a entity that runs on a host that can be reached by peers over a greater distance, e.g.
 * Internet, maybe TCP (why not TOR...). Peer can register with a hub and wait for other peers to establish a
 * connection. Hub provide a list of present peers and allow establishing a connection. A hub is an intermediary element
 * that simply reads data from on peer and delivers those data to another one. It is not meant to store anything.
 * It is a kind of relay.
 * <br/><br/>
 * ASAP is always about trust. You must trust that particular hub implementation that it really does not spy on you.
 * This project offers a hub implementation. You don't have to trust it. You can read the code. It is open source.
 * <br/><br/>
 * Hub API is the HubConnector. It is meant to run on peers' side. This project also provides code to work with a
 * hub connection as with any other point-to-point connection. There is a little protocol that runs between the
 * hub connector and the actual hub. Use it. It is not rocket science but necessary. Have a look at the implementations.
 * Don't hesitate to contact us.
 *
 * @author Thomas Schwotzer
 * @see HubConnector
 * @see TCPHub
 */
public interface Hub {
    /** first IP package was sent Oct, 29th 1969 */
    int DEFAULT_PORT = 6910;

    /**
     * Connect a peer that runs in the same JVM with the hub and is always present. This peer is meant to act as repeater.
     * @param peer
     */
    void addASAPPeer(ASAPPeer peer);

    /**
     * Disconnect peer that runs in the same JVM. It is most probably shut down afterwards.
     * @param peer
     */
    void removeASAPPeer(ASAPPeer peer);
}
