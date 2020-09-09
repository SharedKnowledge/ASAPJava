package net.sharksystem.asap.sharknet;

public interface SharkNetMessageListener {
    void messageReceived(byte[] message, CharSequence topic, CharSequence senderID, boolean verified, boolean encrypted);
}
