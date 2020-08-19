package net.sharksystem.asap;

import net.sharksystem.asap.protocol.ASAP_1_0;
import net.sharksystem.asap.protocol.ASAP_AssimilationPDU_1_0;
import net.sharksystem.asap.protocol.ASAP_Interest_PDU_1_0;
import net.sharksystem.asap.protocol.ASAP_OfferPDU_1_0;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author thsc
 */
public interface ASAPProtocolEngine {
    void handleASAPInterest(ASAP_Interest_PDU_1_0 asapInterest, ASAP_1_0 protocol, OutputStream os)
            throws ASAPException, IOException;

    void handleASAPOffer(ASAP_OfferPDU_1_0 asapOffer, ASAP_1_0 protocol, OutputStream os)
            throws ASAPException, IOException;

    void handleASAPAssimilate(ASAP_AssimilationPDU_1_0 asap_assimilation, ASAP_1_0 protocol,
                              InputStream is, OutputStream os, ASAPChunkReceivedListener listener)
            throws ASAPException, IOException;

    /**
     * Chunks are (tried to be) delivered to their recipients during each encounter
     * with another peer. After successful delivery, recipient is withdrawn from recipient
     * list to prevent multiple delivery.
     *
     * If this flag is set, chunk are removed permanently if their are delivered
     * to all their recipients. There are kept in local storage otherwise.
     * @param drop
     */
    void setBehaviourDropDeliveredChunks(boolean drop) throws IOException;

    /**
     * engine can deliver local message but also received messages - default false - send no received messages
     * @param on
     */
    void setBehaviourSendReceivedChunks(boolean on) throws IOException;
}
