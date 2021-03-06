package net.sharksystem.hub;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class ConnectPDU extends HubPDU {
    public CharSequence peerID;

    public ConnectPDU(CharSequence peerID) {
        this.peerID = peerID;
    }

    public ConnectPDU(InputStream is) throws IOException, ASAPException {
        this.peerID = ASAPSerialization.readCharSequenceParameter(is);
    }

    @Override
    void sendPDU(OutputStream os) throws IOException {
        ASAPSerialization.writeByteParameter(HubPDU.CONNECT_PDU, os);
        ASAPSerialization.writeCharSequenceParameter(this.peerID, os);
    }
}
