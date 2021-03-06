package net.sharksystem.hub;

import net.sharksystem.asap.utils.ASAPSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class RequestInfoPDU extends HubPDU {
    public RequestInfoPDU(InputStream is) {
    }

    public RequestInfoPDU() {
    }

    @Override
    void sendPDU(OutputStream os) throws IOException {
        ASAPSerialization.writeByteParameter(HubPDU.REQUEST_INFOS_PDU, os);
    }
}
