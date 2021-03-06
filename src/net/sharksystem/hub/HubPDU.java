package net.sharksystem.hub;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

abstract class HubPDU {
    static final byte HELLO_PDU = 0;
    static final byte CONNECT_PDU = 1;
    static final byte NEW_CONNECTION_PDU = 2;
    static final byte PROVIDE_INFOS_PDU = 3;
    static final byte REQUEST_INFOS_PDU = 4;

    public static HubPDU readPDU(InputStream is) throws IOException, ASAPException {
        byte b = ASAPSerialization.readByte(is);
        switch (b) {
            case HELLO_PDU: return new HelloPDU(is);
            case CONNECT_PDU: return new ConnectPDU(is);
            case NEW_CONNECTION_PDU: return new NewConnectionPDU(is);
            case PROVIDE_INFOS_PDU: return new ProvideHubInfoPDU(is);
            case REQUEST_INFOS_PDU: return new RequestInfoPDU(is);

            default: throw new IOException("unknown pdu type: " + b);
        }
    }

    abstract void sendPDU(OutputStream os) throws IOException;
}
