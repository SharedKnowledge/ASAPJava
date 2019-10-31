package net.sharksystem.asap.protocol;

public interface ASAP_PDU_1_0 {
    /**
     * @return ASAP command. Version 1 know three alternatives: interest, offer, assimilate
     */
    byte getCommand();

    /**
     * @return ID of sender of this message
     */
    String getPeer();

    /**
     * @return format A URI style is strongly recommended.
     * In most cases a format will match with an application that deals with one (or more
     * formats)
     *
     */
    String getFormat();

    /**
     * @return A URI representing content (semantics) of this message.
     * It is assumed that multiple messages with same uri are floating around the
     * ASAP network
     */
    String getChannelUri();

    /**
     * @return Sender can send era in which this message was created. Can be helpful
     * in multihop networks to reduce communication costs.
     */
    int getEra();

    /**
     * @return a flag that indicates whether the optional peer parameter was transmitted
     */
    boolean peerSet();
    /**
     * @return a flag that indicates whether the optional channel parameter was transmitted
     */
    boolean channelSet();
    /**
     * @return a flag that indicates whether the optional era parameter was transmitted
     */
    boolean eraSet();
}
