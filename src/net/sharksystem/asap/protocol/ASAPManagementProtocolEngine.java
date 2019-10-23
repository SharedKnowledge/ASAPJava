package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.io.InputStream;

public interface ASAPManagementProtocolEngine {
    /**
     * handle asap management pdu
     * @param asapPDU received pdu
     * @param protocol protocol engine
     * @param is inputstream
     * @throws ASAPException
     * @throws IOException
     * @return true if that pdu was fully executed and no further processing is required
     */
    boolean handleASAPManagementPDU(ASAP_PDU_1_0 asapPDU, ASAP_1_0 protocol,
                                    InputStream is) throws ASAPException, IOException;
}
