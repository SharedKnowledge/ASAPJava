package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;

public interface ASAPOnlineConnection {
    CharSequence getRemotePeer();

    void addOnlineMessageSource(ASAPOnlineMessageSource source);
    void removeOnlineMessageSource(ASAPOnlineMessageSource source);

    boolean isSigned();
}
