package net.sharksystem;

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
}
