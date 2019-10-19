package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPChunkReceivedListener;
import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ASAPManagementProtocolEngine {
    /**
     * handle asap management pdu
     * @param asapPDU received pdu
     * @param protocol protocol engine
     * @param is inputstream
     * @param os outputstream - to send data back
     * @throws ASAPException
     * @throws IOException
     */
    void handleASAPManagementPDU(ASAP_AssimilationPDU_1_0 asapPDU, ASAP_1_0 protocol,
                                 InputStream is, OutputStream os) throws ASAPException, IOException;
}
