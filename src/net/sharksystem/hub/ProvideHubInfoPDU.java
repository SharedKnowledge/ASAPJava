package net.sharksystem.hub;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

class ProvideHubInfoPDU extends HubPDU {
    Set<CharSequence> connectedPeers;

    public ProvideHubInfoPDU(Set<CharSequence> connectedPeers) {
        this.connectedPeers = connectedPeers;
    }

    public ProvideHubInfoPDU(InputStream is) throws IOException, ASAPException {
        this.connectedPeers = ASAPSerialization.readCharSequenceSetParameter(is);
    }

    @Override
    void sendPDU(OutputStream os) throws IOException {
        ASAPSerialization.writeByteParameter(HubPDU.PROVIDE_INFOS_PDU, os);
        ASAPSerialization.writeCharSequenceSetParameter(this.connectedPeers, os);
    }
}
