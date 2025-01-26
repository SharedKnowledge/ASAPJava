package net.sharksystem.asap;

public enum ASAPEncounterConnectionType {
    UNKNOWN (0), AD_HOC_LAYER_2_NETWORK (1), ASAP_HUB (2), INTERNET (3), ONION_NETWORK (4);

    private final int type;

    ASAPEncounterConnectionType(int type) {
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
