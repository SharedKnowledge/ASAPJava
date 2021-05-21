package net.sharksystem.asap;

/**
 * Describes the exchange if an ASAP message from one peer to the other
 */
public interface ASAPHop {
    /**
     * Sender is always a point-to-point sender. There is no get receiver method, though. Hops are kept in a chain.
     * Receiver of the last entry is local peer itself. For all hops in between: Sender of next hop is receiver of
     * the previous hop.
     * @return
     */
    CharSequence sender();

    /**
     * An ASAP message exchange is based on a point-to-point connection. There are different options: Ad-hoc networks,
     * Internet, onion networks etc. This method describes the connection type of this hop.
     * @return
     */
    EncounterConnectionType getConnectionType();

    /**
     * A sender could have signed the point-to-point message transfer. This message returns true if the receiver was
     * able to verify the signature. A false could be indicator for a forged identity or that the receiver has not got
     * sender's public key.
     * @return
     */
    boolean verified();

    /**
     * This point-to-point connection was encrypted.
     * @return
     */
    boolean encrypted();

}
