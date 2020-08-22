package net.sharksystem.asap;

import net.sharksystem.asap.protocol.ASAP_AssimilationPDU_1_0;

interface PeerSecurityAdministrator {
    boolean allowedToCreateChannel(ASAP_AssimilationPDU_1_0 asapAssimiliationPDU);
}
