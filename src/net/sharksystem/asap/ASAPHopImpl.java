package net.sharksystem.asap;

import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.EncounterConnectionType;

public class ASAPHopImpl implements ASAPHop {
    private final CharSequence sender;
    private final boolean verified;
    private final boolean encrypted;
    private final EncounterConnectionType connectionType;

    public ASAPHopImpl(CharSequence sender, boolean verified, boolean encrypted, EncounterConnectionType connectionType) {
        this.sender = sender;
        this.verified = verified;
        this.encrypted = encrypted;
        this.connectionType = connectionType;
    }

    @Override
    public CharSequence sender() {
        return this.sender;
    }

    @Override
    public EncounterConnectionType getConnectionType() {
        return this.connectionType;
    }

    @Override
    public boolean verified() {
        return this.verified;
    }

    @Override
    public boolean encrypted() {
        return this.encrypted;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("sender: ");
        sb.append(this.sender);
        sb.append(" | ");
        sb.append("verified: ");
        sb.append(this.verified);
        sb.append(" | ");
        sb.append("encrypted: ");
        sb.append(this.encrypted);
        sb.append(" | ");
        sb.append("connectionType: ");
        switch (this.connectionType) {
            case ONION_NETWORK: sb.append("onionNetwork"); break;
            case ASAP_HUB: sb.append("ASAP Hub"); break;
            case AD_HOC_LAYER_2_NETWORK: sb.append("Ad-hoc protocol"); break;
            case INTERNET: sb.append("Internet"); break;
            default: sb.append("unknown"); break;
        }

        return sb.toString();
    }
}
