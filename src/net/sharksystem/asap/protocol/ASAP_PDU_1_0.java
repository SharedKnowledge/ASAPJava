package net.sharksystem.asap.protocol;

public interface ASAP_PDU_1_0 {
    String getPeer();
    String getFormat();
    String getChannel();
    int getEra();

    boolean peerSet();
    boolean channelSet();
    boolean eraSet();
}
