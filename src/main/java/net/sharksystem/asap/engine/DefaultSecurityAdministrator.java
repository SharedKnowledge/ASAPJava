package net.sharksystem.asap.engine;

import net.sharksystem.asap.protocol.ASAP_AssimilationPDU_1_0;
import net.sharksystem.asap.protocol.ASAP_PDU_1_0;
import net.sharksystem.asap.crypto.ASAPPoint2PointCryptoSettings;
import net.sharksystem.utils.Log;

import java.io.IOException;

public class DefaultSecurityAdministrator implements ASAPCommunicationSetting,
        ASAPEnginePermissionSettings, CryptoControl, ASAPPoint2PointCryptoSettings {

    private boolean receivedMessageMustBeEncrypted = false;
    private boolean receivedMessagesMustBeSigned = false;
    private boolean sendEncrypted = false;
    private boolean sendSigned;

    @Override
    public void setRememberEncounteredPeers(boolean on) throws IOException {

    }

    @Override
    public void setReceivedMessagesMustBeEncrypted(boolean on) throws IOException {
        this.receivedMessageMustBeEncrypted = on;
    }

    @Override
    public void setReceivedMessagesMustBeSigned(boolean on) throws IOException {
        this.receivedMessagesMustBeSigned = on;
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
        if(this.receivedMessagesMustBeSigned && !pdu.signed()) {
            Log.writeLog(this, "checked: " + pdu);
            Log.writeLog(this, "not signed");
            return false;
        }
        if(this.receivedMessageMustBeEncrypted && !pdu.encrypted()) {
            Log.writeLog(this, "checked: " + pdu);
            Log.writeLog(this, "not encrypted");
            return false;
        }

        Log.writeLog(this, "ok");
        return true;
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }

    @Override
    public boolean mustEncrypt() {
        return this.sendEncrypted;
    }

    @Override
    public boolean mustSign() {
        return this.sendSigned;
    }

    /*
    private boolean encryptedMessagesOnly = false;
    private boolean signedMessagesOnly = false;
    private boolean sendEncrypted = false;
    private boolean sendSigned;

     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getLogStart());
        sb.append("recEncrypted: " + this.receivedMessageMustBeEncrypted);
        sb.append(" | recSigned: " + this.receivedMessagesMustBeSigned);
        sb.append(" | sendEncrypted: " + this.sendEncrypted);
        sb.append(" | sendSigned: " + this.sendSigned);
        return sb.toString();
    }
}
