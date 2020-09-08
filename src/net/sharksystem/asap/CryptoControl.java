package net.sharksystem.asap;

import net.sharksystem.asap.protocol.ASAP_PDU_1_0;

public interface CryptoControl {
    boolean allowed2Process(ASAP_PDU_1_0 pdu);
}
