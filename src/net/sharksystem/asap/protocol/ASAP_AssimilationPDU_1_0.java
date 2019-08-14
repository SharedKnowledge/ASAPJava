package net.sharksystem.asap.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ASAP_AssimilationPDU_1_0 extends ASAP_PDU_1_0 {
    boolean recipientPeerSet();

    String getRecipientPeer();

    byte[] getData();

    void streamData(OutputStream os) throws IOException;
}
