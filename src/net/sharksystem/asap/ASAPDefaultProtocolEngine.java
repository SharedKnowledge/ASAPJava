package net.sharksystem.asap;

import net.sharksystem.asap.protocol.ASAP_1_0;
import net.sharksystem.asap.protocol.ASAP_AssimilationPDU_1_0;
import net.sharksystem.asap.protocol.ASAP_Interest_PDU_1_0;
import net.sharksystem.asap.protocol.ASAP_OfferPDU_1_0;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * TODO: extract protocol engine algorithms here..
 */
public class ASAPDefaultProtocolEngine implements ASAPProtocolEngine {
    private final ASAPStorage asapStorage;

    ASAPDefaultProtocolEngine(ASAPStorage asapStorage) {
        this.asapStorage = asapStorage;
    }

    @Override
    public void handleASAPInterest(ASAP_Interest_PDU_1_0 asapInterest, ASAP_1_0 protocol, OutputStream os) throws ASAPException, IOException {

    }

    @Override
    public void handleASAPOffer(ASAP_OfferPDU_1_0 asapOffer, ASAP_1_0 protocol, OutputStream os) throws ASAPException, IOException {

    }

    @Override
    public void handleASAPAssimilate(ASAP_AssimilationPDU_1_0 asap_assimilation, ASAP_1_0 protocol, InputStream is, OutputStream os, ASAPChunkReceivedListener listener) throws ASAPException, IOException {

    }

    @Override
    public void setBehaviourDropDeliveredChunks(boolean drop) throws IOException {

    }

    @Override
    public void setBehaviourSendReceivedChunks(boolean on) throws IOException {

    }
}
