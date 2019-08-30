package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;

public interface ASAPConnection {
    CharSequence getRemotePeer();

    void addMessage(CharSequence recipient, CharSequence format, CharSequence urlTarget,
                    byte[] messageAsBytes, int era) throws IOException, ASAPException;

    boolean isSigned();
}
