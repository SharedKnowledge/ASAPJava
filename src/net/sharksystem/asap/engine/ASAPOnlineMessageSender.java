package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.util.Set;

public interface ASAPOnlineMessageSender {
    void sendASAPAssimilateMessage(CharSequence format, CharSequence urlTarget, Set<CharSequence> recipients,
                                   byte[] messageAsBytes, int era) throws IOException, ASAPException;

    void sendASAPAssimilateMessage(CharSequence format, CharSequence urlTarget, CharSequence recipient,
                                   byte[] messageAsBytes, int era) throws IOException, ASAPException;

    void sendASAPAssimilateMessage(CharSequence format, CharSequence urlTarget, byte[] messageAsBytes, int era)
            throws IOException, ASAPException;

    void sendASAPAssimilateMessage(CharSequence format, CharSequence urlTarget, byte[] messageAsBytes)
            throws IOException, ASAPException;
}
