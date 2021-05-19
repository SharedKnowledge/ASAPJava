package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static net.sharksystem.asap.protocol.ASAP_1_0.ERA_NOT_DEFINED;

abstract class PDU_Impl implements ASAP_PDU_1_0, ASAP_PDU_Management {
    public static final int SENDER_BIT_POSITION = 0;
    public static final int RECIPIENT_BIT_POSITION = 1;
    public static final int CHANNEL_BIT_POSITION = 2;
    public static final int ERA_BIT_POSITION = 3;
    public static final int ERA_FROM_BIT_POSITION = 4;
    public static final int ERA_TO_BIT_POSITION = 5;
    public static final int OFFSETS_BIT_POSITION = 6;
    public static final int SIGNED_TO_BIT_POSITION = 7;
    public static final int ROUTING_BIT_POSITION = 8;
    public static final int ENCOUNTER_MAP_BIT_POSITION = 9;

    private boolean senderSet = false;
    private boolean recipientSet = false;
    private boolean channelSet = false;
    private boolean eraSet = false;
    private boolean eraFrom = false;
    private boolean eraTo = false;
    private boolean offsetsSet = false;
    private boolean routing = false;
    private boolean encounterList = false;

    private final byte cmd;
    private final boolean encrypted;

    private boolean signed = false;
    private boolean verified = false;

    private String sender;
    private String recipient;
    private String format;
    private String channel;
    private int era;

    PDU_Impl(byte cmd, boolean encrypted) {
        this.cmd = cmd;
        this.encrypted = encrypted;
    }

    public boolean encrypted() { return this.encrypted; }
    public boolean signed() { return this.signed; }
    public boolean verified() { return this.verified; };
    public boolean routing() { return this.routing; };
    public boolean encounterList() { return this.encounterList; };

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("cmd: ");
        switch(cmd) {
            case ASAP_1_0.INTEREST_CMD: sb.append("I"); break;
            case ASAP_1_0.ASSIMILATE_CMD: sb.append("A"); break;
        }
        sb.append(" | sender: "); if(senderSet) sb.append(this.sender); else sb.append("not set");
        sb.append(" | format: "); sb.append(format);
        sb.append(" | channel: "); if(channelSet) sb.append(this.channel); else sb.append("not set");
        sb.append(" | era: "); if(eraSet) sb.append(era); else sb.append("not set");
        sb.append(" | signed: "); this.appendTrueFalse(this.signed, sb);
        sb.append(" | verified: "); this.appendTrueFalse(this.verified, sb);
        sb.append(" | encrypted: "); this.appendTrueFalse(this.encrypted, sb);
        sb.append(" | routing: "); this.appendTrueFalse(this.routing, sb);
        sb.append(" | encounterList: "); this.appendTrueFalse(this.encounterList, sb);

        return sb.toString();
    }

    private void appendTrueFalse(boolean value, StringBuilder sb) {
        if(value) sb.append("true");
        else sb.append("false");
    }

    /**
     * @param cmd
     * @param flags
     * @param os
     * @throws IOException
     * @deprecated
     */
    protected static void sendHeader(byte cmd, int flags, OutputStream os) throws IOException {
        ASAPSerialization.writeByteParameter(cmd, os); // mand
        ASAPSerialization.writeByteParameter((byte)flags, os); // mand
    }

    public static void sendFlags(int flags, OutputStream os) throws IOException {
        // we need two bytes - sent least signification byte first
        byte flagBytes = ASAPSerialization.getByteFromInt(flags, 0);
        ASAPSerialization.writeByteParameter(flagBytes, os); // mand
        flagBytes = ASAPSerialization.getByteFromInt(flags, 1);
        ASAPSerialization.writeByteParameter(flagBytes, os); // mand
    }

    public static int readFlags(InputStream is) throws IOException, ASAPException {
        byte byteLeastSignificant = ASAPSerialization.readByte(is);
        byte byteNextByte = ASAPSerialization.readByte(is);

        int flags = byteNextByte;
        flags = flags << 8;
        // set all random bits to 0
        flags = flags & 0x0000FF00;

        int leastSignificantInt = byteLeastSignificant;
        // set all random bits to 0
        leastSignificantInt = leastSignificantInt & 0x000000FF;

        // merge
        flags = flags | leastSignificantInt;

        return flags;
    }

    public static void sendCmd(byte cmd, OutputStream os) throws IOException {
        ASAPSerialization.writeByteParameter(cmd, os); // mand
    }

    protected void evaluateFlags(int flag) {
        this.senderSet = flagSet(SENDER_BIT_POSITION, flag);
        this.recipientSet = flagSet(RECIPIENT_BIT_POSITION, flag);
        this.channelSet = flagSet(CHANNEL_BIT_POSITION, flag);
        this.eraSet = flagSet(ERA_BIT_POSITION, flag);
        this.eraFrom = flagSet(ERA_FROM_BIT_POSITION, flag);
        this.eraTo = flagSet(ERA_TO_BIT_POSITION, flag);
        this.offsetsSet = flagSet(OFFSETS_BIT_POSITION, flag);
        this.signed = flagSet(SIGNED_TO_BIT_POSITION, flag);
        this.routing = flagSet(ROUTING_BIT_POSITION, flag);
        this.encounterList = flagSet(ENCOUNTER_MAP_BIT_POSITION, flag);
    }

    static boolean flagSet(int bitPosition, int flags) {
        int flagMask = 1;
        flagMask = flagMask << bitPosition;
        return (flags & flagMask) != 0;
    }

    @Override
    public boolean senderSet() { return this.senderSet; }

    @Override
    public boolean channelSet() { return this.channelSet; }

    @Override
    public boolean eraSet() { return this.eraSet; }

    public boolean eraFromSet() { return this.eraFrom; }

    public boolean eraToSet() { return this.eraTo; }

    public boolean recipientSet() { return this.recipientSet; }

    public boolean offsetsSet() { return this.offsetsSet; }

    @Override
    public String getSender() { return this.sender; }

    @Override
    public String getRecipient() { return this.recipient; }

    @Override
    public String getFormat() { return this.format; }

    @Override
    public String getChannelUri() { return this.channel; }

    @Override
    public int getEra() { return this.era;}


    @Override
    public byte getCommand() { return this.cmd; }

    protected void readSender(InputStream is) throws IOException, ASAPException {
        this.sender = ASAPSerialization.readCharSequenceParameter(is);
    }

    protected void readRecipient(InputStream is) throws IOException, ASAPException {
        this.recipient = ASAPSerialization.readCharSequenceParameter(is);
    }

    protected void readFormat(InputStream is) throws IOException, ASAPException {
        this.format = ASAPSerialization.readCharSequenceParameter(is);
    }


    protected void readChannel(InputStream is) throws IOException, ASAPException {
        this.channel = ASAPSerialization.readCharSequenceParameter(is);
    }

    protected void readEra(InputStream is) throws IOException, ASAPException {
        this.era = ASAPSerialization.readIntegerParameter(is);
    }

    static void sendCommand(byte cmd, OutputStream os) throws IOException {
        os.write(cmd);
    }

    /**
     * Set flag of parameter is set and will be transmitted
     * @param flags
     * @param parameter
     * @param bit_position
     * @return
     */
    static int setFlag(CharSequence parameter, int flags, int bit_position) {
        if(parameter != null && parameter.length() > 0) {
            return setFlag(1, flags, bit_position);
        }
        return flags;
    }

    static int setFlag(List<Long> parameter, int flags, int bit_position) {
        if(parameter != null && parameter.size() > 0) {
            return setFlag(1, flags, bit_position);
        }
        return flags;
    }

    static int setFlag(int parameter, int flags, int bit_position) {
        if(parameter != ERA_NOT_DEFINED) {
            int newFlag = 1;
            newFlag = newFlag << bit_position;

            return flags | newFlag;
        }

        return flags;
    }

    static int setFlag(boolean parameter, int flags, int bit_position) {
        if(parameter) {
            int newFlag = 1;
            newFlag = newFlag << bit_position;

            return flags | newFlag;
        }

        return flags;
    }

    static void checkValidStream(OutputStream os) throws ASAPException {
        if(os == null) throw new ASAPException("outputstream must not be null");
    }

    static void checkValidFormat(CharSequence format) throws ASAPException {
        if(format == null) throw new ASAPException("format must not be null");
    }

    static void checkValidEra(int era) throws ASAPException {
        if(era < -1) throw new ASAPException("era cannot be smaller than -1");
        int maxEra = Integer.MAX_VALUE;
        // that's impossible but ... never change a running system...
        if(era > maxEra) throw new ASAPException("era exceeded max limit of " + Integer.MAX_VALUE);
    }
}
