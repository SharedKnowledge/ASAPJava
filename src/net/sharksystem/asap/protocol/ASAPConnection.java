package net.sharksystem.asap.protocol;

public interface ASAPConnection {
    CharSequence getEncounteredPeer();

    void addOnlineMessageSource(ASAPOnlineMessageSource source);
    void removeOnlineMessageSource(ASAPOnlineMessageSource source);

    void addASAPConnectionListener(ASAPConnectionListener asapConnectionListener);

    void removeASAPConnectionListener(ASAPConnectionListener asapConnectionListener);

    boolean isSigned();

    // terminate that connection - does not effect the underlying connections established e.g. with Bluetooth
    void kill();
}
