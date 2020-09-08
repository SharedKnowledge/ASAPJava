package net.sharksystem.asap;

import net.sharksystem.asap.protocol.ASAP_AssimilationPDU_1_0;
import net.sharksystem.asap.protocol.ASAP_PDU_1_0;
import net.sharksystem.crypto.ASAPCommunicationCryptoSettings;

import java.io.IOException;

public class DefaultSecurityAdministrator implements ASAPCommunicationSetting,
        ASAPEnginePermissionSettings, PermissionControl, ASAPCommunicationCryptoSettings {

    private boolean encryptedMessagesOnly = false;
    private boolean signedMessagesOnly = false;
    private boolean sendEncrypted = false;
    private boolean sendSigned;

    @Override
    public void setRememberEncounteredPeers(boolean on) throws IOException {

    }

    @Override
    public void setReceivedMessagesMustBeEncrypted(boolean on) throws IOException {
        this.encryptedMessagesOnly = on;
    }

    @Override
    public void setReceivedMessagesMustBeSigned(boolean on) throws IOException {
        this.signedMessagesOnly = on;
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

    @Override
    public boolean allowedToCreateChannel(ASAP_AssimilationPDU_1_0 asapAssimilationPDU) {
        return true; // it is a dummy
    }

    @Override
    public void setSendEncryptedMessages(boolean on) {
        this.sendEncrypted = on;
    }

    @Override
    public void setSendSignedMessages(boolean on) {
        this.sendSigned = on;
    }

    @Override
    public boolean allowed2Process(ASAP_PDU_1_0 pdu) {
        if(this.signedMessagesOnly && !pdu.signed()) return false;
        if(this.encryptedMessagesOnly && !pdu.encrypted()) return false;

        return true;
    }

    @Override
    public boolean mustEncrypt() {
        return this.sendEncrypted;
    }

    @Override
    public boolean mustSign() {
        return this.sendSigned;
    }
}
