package net.sharksystem.asap.protocol;

public interface ASAPConnection {
    CharSequence getRemotePeer();

    void addOnlineMessageSource(ASAPOnlineMessageSource source);
    void removeOnlineMessageSource(ASAPOnlineMessageSource source);

    boolean isSigned();

    // terminate that connection - does not effect the underlying connections established e.g. with Bluetooth
    void kill();
}
