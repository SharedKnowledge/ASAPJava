package net.sharksystem.asap.sharknet;

import net.sharksystem.asap.ASAPConnectionHandler;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;

import java.io.IOException;

public interface SharkNetPeer extends ASAPConnectionHandler {
    CharSequence getOwnerID();

    void sendSharkNetMessage(byte[] message, CharSequence topic, CharSequence recipient,
                                    boolean sign, boolean encrypted) throws IOException, ASAPException;
    void addSharkNetMessageListener(SharkNetMessageListener snLister);
    void removeSharkNetMessageListener(SharkNetMessageListener snLister);
}
