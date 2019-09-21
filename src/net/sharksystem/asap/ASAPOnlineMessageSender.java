package net.sharksystem.asap;

import java.io.IOException;
import java.util.List;

public interface ASAPOnlineMessageSender {
    void sendASAPAssimilate(CharSequence format, CharSequence urlTarget, List<CharSequence> recipients,
                            byte[] messageAsBytes, int era) throws IOException, ASAPException;

    void sendASAPAssimilate(CharSequence format, CharSequence urlTarget, CharSequence recipient,
                            byte[] messageAsBytes, int era) throws IOException, ASAPException;

    void sendASAPAssimilate(CharSequence format, CharSequence urlTarget, byte[] messageAsBytes, int era)
            throws IOException, ASAPException;
}
