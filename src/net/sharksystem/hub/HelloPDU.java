package net.sharksystem.hub;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class HelloPDU extends HubPDU {
    final CharSequence peerID;

    HelloPDU(CharSequence peerID) {
        this.peerID = peerID;
    }

    HelloPDU(InputStream is) throws IOException, ASAPException {
        this.peerID = ASAPSerialization.readCharSequenceParameter(is);
    }

    @Override
    void sendPDU(OutputStream os) throws IOException {
        ASAPSerialization.writeByteParameter(HubPDU.HELLO_PDU, os);
        ASAPSerialization.writeCharSequenceParameter(this.peerID, os);
    }
}
