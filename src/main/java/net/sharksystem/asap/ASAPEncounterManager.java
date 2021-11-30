package net.sharksystem.asap;

import net.sharksystem.streams.StreamPair;

import java.io.IOException;

/**
 * An ASAP peer can handle ASAP encounter as they come. It takes in- and output stream and runs an ASAP session.
 * Where are those streams from? They are created from underlying protocol engines. There are probably and hopefully
 * more than one protocol engines in a real environment. A smartphone would provide Bluetooth, Wifi direct and
 * maybe it has allows communicating via an ASAPHub.
 * <br/><br/>
 * We could of course ignore this somewhat more complex environment and create a connection whenever possible and
 * run an ASAP session. That not very efficient, though. It has even the tendency to be a considerable wast of
 * resources. Why is that?
 * <br/><br/>
 * Take ad-hoc networks as an example: A connection can be established within a given radius, like 10m with Bluetooth.
 * A connection gets lost if two devices are over that threshold and can be re-established if they get closer. This
 * leads to flickering effects. Connection gone / re-established / gone again etc. That's ok and useful if we are about
 * implementing streaming services. But we war not. ASAP is basically meant to be platform for distributed apps which
 * are based on the command pattern. There are no streams but data packages.
 * <br/><br/>
 * Application developers will have a pretty good understanding of how often such messages are created. Even very fast
 * human writers will barely produce more than a message per second, presumably far less. Exchange rate in a sensor
 * network observing outside temperature is better be measured in hours than minutes. This will be different in
 * industrial processes. Apparently: It depends on the application scenario. We want to give application developers
 * as much help for optimization as possible. That is one way to safe most probably a lot of energy.
 * <br/><br/>
 * ASAP is an opportunistic protocol. Connections should be established whenever possible. This could and will be a
 * serious leak on resources in a multi-protocol environment. It is hardly useful that two smartphones create a
 * Bluetooth <i>and</i> a Wifi-direct connection if they can. One is sufficient.
 * <br/><br/>
 * An encounter manager deals with those problems. It is highly recommended to use it. We do, see e.g. our ASAPAndroid
 * lib which is even more highly recommend.
 */
public interface ASAPEncounterManager {
    /**
     * Other devices can be detected by a point-to-point protocol. This method is to be called before a connection
     * is established.
     * <br/><br/>
     * It can be checked when a last encounter happened. This is most useful e.g. in ad-hoc networks. Communication can
     * get lost by a (slight) movement and could be reestablished a moment later. A kind of cool-down-period can be
     * defined.
     * <br/><br/>
     * Peers could communicate over different channels. Same argument: It can be decided <i>not</i> to establish
     * a connection due to an existing one over another protocol.
     * @param addressOrPeerID address of remote device or a peer id. It is only used as an index, an id.
     *                      There will be no attempts to establish a connection.
     * @param connectionType Describes the connection that is planned to be established.
     * @return true: a connection should be established or not.
     * @see EncounterConnectionType
     */
    boolean shouldCreateConnectionToPeer(CharSequence addressOrPeerID, EncounterConnectionType connectionType);

    /**
     * A connection to another device was established. This method can be called to handle this new connection.
     * It can lead to an immediate shutdown, e.g. the other device already made a successful attempt to connect e.g.
     * over another protocol.
     * <br/><br/>
     * In a lot of cases an ASAP session will be launched.
     *
     * @param streamPair most likely a socket that is can be used for an ASAP session.
     * @throws IOException something can go wrong when sending data. This object has no obligation to deal with this.
     */
    void handleEncounter(StreamPair streamPair, EncounterConnectionType connectionType) throws IOException;

    /**
     * This method is a variant of the one with less parameters.
     *
     * Here is the catch:
     * <br/><br/>
     * We have a race condition in any ad-hoc network based on this library. What happens if both devices come into
     * sending range?
     * <br/><br/>
     * <ul>
     *     <li>Each asks this object if it should establish a connection by calling shouldConnect. It is highly likely
     *     that both side get same answer. Let's assume it is a yes.</li>
     *     <li>Both would open a passive endpoint (most likely a server socket). In most cases it is already open
     *     before the previous step. In any case..</li>
     *     <li>Both would try to establish a connection to the other side... An here starts the race...</li>
     *     <li>Let's assume Alice and Bob are in that situation: Alice would recognize an connection attempt from
     *     Bob on her server socket. She is also aware that she is also doing a connection attempt to Bob. That's in the
     *     nature of this process. We do not want this. We do not want to parallel connections between those two devices.
     *     One should be canceled. But what: Alice -> Bob or Bob -> Alice ?</li>
     *     <li>Bob is in the same situation. That problem arose when working with Bluetooth. It happens very often.
     *     This simple solution does <i>not</i> work: If we see a connection attempt on our server we drop out active
     *     connection attempt. Both side would do the same and both connection attempts are canceled. That is the race
     *     condition.
     *     </li>
     * </ul>
     *
     * This problem can be solved by adding an additional parameter. This boolean value states if this method is
     * called by an <i>initiator</i>. What is that? A process that create e.g. a client socket is - per definition -
     * an initiator. It initiated this connection. A server socket waits for connection attempts and is
     * - per definition - no initiator. Please, choose very carefully this parameter. The race condition is solved
     * by an implementation of this interface.
     * <br/><br/>
     * If you are interested: Here is an idea of an implementation.
     * <ul>
     *     <li>It is checked if there is an open connection to the other side before anything other happens.
     *     If so: stream pair is closed and we are ready here.</li>
     *     <li>Both sides exchange a random value over this stream pair. One side will be initiator - the other not.
     *     (If you chose that parameter carefully). No we can decide:
     *     What is the random number of the initiator and what is the non-initiator value.</li>
     *     <li>Is the initiator value smaller than the non-initial value? If so:
     *     We let this thread sleep a little while. We have produced an asymmetry. One process waits, the other not.
     *     </li>
     *     <li>After wake-up: We check again if there is already a connection established. If so - there was a race
     *     condition and we solved it. If not - we establish a connection.</li>
     * </ul>
     *
     * You should call this variant in any ad-hoc network. You should also uses this variant if you have a fully
     * symmetric situation - both devices offer a port to connect to any make and active attempt to connect.
     *
     * @param streamPair
     * @param initiator
     * @throws IOException
     * @see ASAPEncounterManager#handleEncounter(StreamPair, EncounterConnectionType)
     */
    void handleEncounter(StreamPair streamPair, EncounterConnectionType connectionType, boolean initiator) throws IOException;
}
