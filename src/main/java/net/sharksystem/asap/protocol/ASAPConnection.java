package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPEncounterConnectionType;

public interface ASAPConnection {
    CharSequence getEncounteredPeer();

    void addOnlineMessageSource(ASAPOnlineMessageSource source);
    void removeOnlineMessageSource(ASAPOnlineMessageSource source);

    void addASAPConnectionListener(ASAPConnectionListener asapConnectionListener);

    void removeASAPConnectionListener(ASAPConnectionListener asapConnectionListener);

    boolean isSigned();

    ASAPEncounterConnectionType getASAPEncounterConnectionType();

    // terminate that connection - does not effect the underlying connections established e.g. with Bluetooth
    void kill();
}
