package net.sharksystem.asap;

public enum ASAPEncounterConnectionType {
    UNKNOWN ((byte) 0),
    AD_HOC_LAYER_2_NETWORK ((byte) 1),
    ASAP_HUB ((byte) 2),
    INTERNET ((byte) 3),
    ONION_NETWORK ((byte) 4);

    private final byte type;

    ASAPEncounterConnectionType(byte type) {
        this.type = type;
    }

    public String toString() {
        switch(this.type) {
            case 0: return "unknown";
            case 1: return "ad-hoc";
            case 2: return "hub";
            case 3: return "internet";
            case 4: return "onion";
            default: return "<there is no type " + this.type;
        }
    }
}
