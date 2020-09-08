package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

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

    private boolean senderSet = false;
    private boolean recipientSet = false;
    private boolean channelSet = false;
    private boolean eraSet = false;
    private boolean eraFrom = false;
    private boolean eraTo = false;
    private boolean offsetsSet = false;

    private final boolean encrypted;
    private final byte cmd;

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

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("cmd: ");
        switch(cmd) {
            case ASAP_1_0.INTEREST_CMD: sb.append("I"); break;
            case ASAP_1_0.OFFER_CMD: sb.append("O"); break;
            case ASAP_1_0.ASSIMILATE_CMD: sb.append("A"); break;
        }
        sb.append(" | sender: "); if(senderSet) sb.append(this.sender); else sb.append("not set");
        sb.append(" | format: "); sb.append(format);
        sb.append(" | channel: "); if(channelSet) sb.append(this.channel); else sb.append("not set");
        sb.append(" | era: "); if(eraSet) sb.append(era); else sb.append("not set");
        sb.append(" | signed: "); this.appendTrueFalse(this.signed, sb);
        sb.append(" | verified: "); this.appendTrueFalse(this.verified, sb);
        sb.append(" | encrypted: "); this.appendTrueFalse(this.encrypted, sb);

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
        PDU_Impl.sendByteParameter(cmd, os); // mand
        PDU_Impl.sendByteParameter((byte)flags, os); // mand
    }

    public static void sendFlags(int flags, OutputStream os) throws IOException {
        PDU_Impl.sendByteParameter((byte)flags, os); // mand
    }

    public static void sendCmd(byte cmd, OutputStream os) throws IOException {
        PDU_Impl.sendByteParameter(cmd, os); // mand
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


    static protected void sendCharSequenceParameter(CharSequence parameter, OutputStream os) throws IOException {
        if(parameter == null || parameter.length() < 1) return;
        byte[] bytes = parameter.toString().getBytes();
        sendNonNegativeIntegerParameter(bytes.length, os);
        os.write(bytes);
    }

    static void sendByteParameter(byte parameter, OutputStream os) throws IOException {
        os.write(new byte[] { parameter} );
    }


    static void sendShortParameter(short parameter, OutputStream os) throws IOException {
        // short = 16 bit = 2 bytes
        int leftInt = parameter >> 8;
        sendByteParameter( (byte)leftInt, os);
        // cut left part
        sendByteParameter( (byte)parameter, os);
    }

    static void sendNonNegativeIntegerParameter(int parameter, OutputStream os) throws IOException {
        if(parameter < 0) return; // non negative!

        // Integer == 32 bit == 4 Byte
        int left = parameter >> 16;
        sendShortParameter((short) left, os);
        sendShortParameter((short) parameter, os);
    }

    protected static void sendNonNegativeLongParameter(long longValue, OutputStream os) throws IOException {
        if(longValue < 0) return;

        // Long = 64 bit = 2 Integer
        long left = longValue >> 32;
        sendNonNegativeIntegerParameter((int) left, os);
        sendNonNegativeIntegerParameter((int) longValue, os);
    }

    static byte readByteParameter(InputStream is) throws IOException, ASAPException {
        return PDU_Impl.readByte(is);
    }

    static byte readByte(InputStream is) throws IOException, ASAPException {
        int value = is.read();
        if(value < 0) {
            throw new ASAPException("read -1: no more data in stream");
        }
        return (byte) value;
    }

    static short readShortParameter(InputStream is) throws IOException, ASAPException {
        int value = readByteParameter(is);
        value = value << 8;
        int right = readByteParameter(is);
        value += right;
        return (short) value;
    }

    static int readIntegerParameter(InputStream is) throws IOException, ASAPException {
        int value = readShortParameter(is);
        value = value << 16;
        int right = readShortParameter(is);
        value += right;
        return value;
    }

    static long readLongParameter(InputStream is) throws IOException, ASAPException {
        long value = readIntegerParameter(is);
        value = value << 32;
        long right = readIntegerParameter(is);
        value += right;
        return value;
    }

    static String readCharSequenceParameter(InputStream is) throws IOException, ASAPException {
        int length = readIntegerParameter(is);
        byte[] parameterBytes = new byte[length];

        is.read(parameterBytes);

        return new String(parameterBytes);
    }


    protected void readSender(InputStream is) throws IOException, ASAPException {
        this.sender = this.readCharSequenceParameter(is);
    }

    protected void readRecipient(InputStream is) throws IOException, ASAPException {
        this.recipient = this.readCharSequenceParameter(is);
    }

    protected void readFormat(InputStream is) throws IOException, ASAPException {
        this.format = this.readCharSequenceParameter(is);
    }


    protected void readChannel(InputStream is) throws IOException, ASAPException {
        this.channel = this.readCharSequenceParameter(is);
    }

    protected void readEra(InputStream is) throws IOException, ASAPException {
        this.era = this.readIntegerParameter(is);
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
