package net.sharksystem.asap.engine;

import net.sharksystem.asap.EncounterConnectionType;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.protocol.ASAP_1_0;
import net.sharksystem.asap.protocol.ASAP_AssimilationPDU_1_0;
import net.sharksystem.asap.protocol.ASAP_Interest_PDU_1_0;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author thsc
 */
public interface ASAPProtocolEngine {
    void handleASAPInterest(ASAP_Interest_PDU_1_0 asapInterest, ASAP_1_0 protocol,
                            String encounteredPeer, OutputStream os, EncounterConnectionType connectionType)
            throws ASAPException, IOException;

    void handleASAPAssimilate(ASAP_AssimilationPDU_1_0 asapAssimilationPDU, ASAP_1_0 protocolModem,
                              String encounteredPeer, InputStream is, OutputStream os,
                              EncounterConnectionType connectionType,
                              ASAPChunkReceivedListener listener)
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
    void setBehaviourAllowRouting(boolean on) throws IOException;

    boolean routingAllowed();
}
