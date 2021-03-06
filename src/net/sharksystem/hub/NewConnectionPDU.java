package net.sharksystem.hub;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class NewConnectionPDU extends HubPDU {
    CharSequence peerID;
    int port;

    public NewConnectionPDU(InputStream is) throws IOException, ASAPException {
        this.port = ASAPSerialization.readIntegerParameter(is);
        this.peerID = ASAPSerialization.readCharSequenceParameter(is);
    }

    public NewConnectionPDU(int port, CharSequence peerID) {
        this.port = port;
        this.peerID = peerID;
    }

    @Override
    void sendPDU(OutputStream os) throws IOException {
        ASAPSerialization.writeByteParameter(HubPDU.NEW_CONNECTION_PDU, os);
        ASAPSerialization.writeIntegerParameter(this.port, os);
        ASAPSerialization.writeCharSequenceParameter(this.peerID, os);
    }
}
