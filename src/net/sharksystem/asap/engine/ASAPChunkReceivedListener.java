package net.sharksystem.asap.engine;

import net.sharksystem.EncounterConnectionType;

import java.io.IOException;

/**
 *
 * @author thsc
 */
public interface ASAPChunkReceivedListener {
    /**
     * Announce incoming ASAP messages. Note the important difference of both senders. ASAP is a routing protocol.
     * An ASAP message has got an original sender (the end-to-end (E2E) sender). Other peers can route this message.
     * The final (senderPoint2Point) is not necessarily the E2E sender but the peer on the other side of the channel.
     * <br/><br/>
     * E2E sender and direct sender are always the same when disengaging routing.
     * It is recommended to allow routing, though.
     *
     * @param format application format
     * @param senderE2E original sender peer - the E2E sender - not necessarily the sender on the other side of
     *               the channel.
     * @param uri message uri
     * @param era era of the original sender (the end-to-end sender). not
     * @param senderPoint2Point the sender on the other side of the channel.
     * @param verified true only if a) point-to-point connection was signed and b) signature could be verified
     * @param encrypted true if point-to-point connection was encrypted
     * @param connectionType describes the type of point-to-point connection (ad-hoc network, Internet, an
     *                       onion network, like TOR etc.)
     * @see EncounterConnectionType
     */
    void chunkReceived(String format, String senderE2E, String uri, int era, // E2E part
                       String senderPoint2Point, boolean verified, boolean encrypted, // Point2Point part
                       EncounterConnectionType connectionType) throws IOException;
}
