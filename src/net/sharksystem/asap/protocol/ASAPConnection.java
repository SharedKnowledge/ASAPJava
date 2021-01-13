package net.sharksystem.asap.protocol;

public interface ASAPConnection {
    CharSequence getRemotePeer();

    void addOnlineMessageSource(ASAPOnlineMessageSource source);
    void removeOnlineMessageSource(ASAPOnlineMessageSource source);

    boolean isSigned();

    // terminate that connection - don't do anything with underlying protocol (streams)
    void kill();
}
