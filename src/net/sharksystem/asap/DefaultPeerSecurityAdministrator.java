package net.sharksystem.asap;

import java.io.IOException;

public class DefaultPeerSecurityAdministrator implements PeerSecurityAdministrator, PeerSecuritySettings {
    @Override
    public void setRememberEncounteredPeers(boolean on) throws IOException {

    }

    @Override
    public void setEncryptedMessagesOnly(boolean on) throws IOException {

    }

    @Override
    public void setSignedMessagesOnly(boolean on) throws IOException {

    }

    @Override
    public void setSetAllowedRemotePeers(AllowedRemotePeers safetyLevel) {

    }

    @Override
    public boolean setRevealEngineFormat(String peerName) {
        return false;
    }

    @Override
    public boolean setSendOpenMessages(String peerName) {
        return false;
    }
}
