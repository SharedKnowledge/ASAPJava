package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

abstract class PDU_Impl implements ASAP_PDU_1_0{
    public static final int PEER_BIT_POSITION = 0;
    public static final int CHANNEL_BIT_POSITION = 1;
    public static final int ERA_BIT_POSITION = 2;
    public static final int SOURCE_PEER_BIT_POSITION = 3;
    public static final int ERA_FROM_BIT_POSITION = 4;
    public static final int ERA_TO_BIT_POSITION = 5;
    public static final int RECIPIENT_PEER_BIT_POSITION = 6;
    public static final int OFFSETS_BIT_POSITION = 7;

    private boolean peerSet = false;
    private boolean channelSet = false;
    private boolean eraSet = false;
    private boolean sourcePeerSet = false;
    private boolean eraFrom = false;
    private boolean eraTo = false;
    private boolean recipientPeerSet = false;
    private boolean offsetsSet = false;

    private String peer;
    private String format;
    private String channel;
    private int era;
    private final byte cmd;

    PDU_Impl(byte cmd) {
        this.cmd = cmd;
    }

    protected static void sendHeader(byte cmd, int flags, OutputStream os) throws IOException {
        PDU_Impl.sendByteParameter(cmd, os); // mand
        PDU_Impl.sendByteParameter((byte)flags, os); // mand
    }

    protected void evaluateFlags(int flag) {
        // peer parameter set ?
        int testFlag = 1;
        testFlag = testFlag << PEER_BIT_POSITION;
        int result = flag & testFlag;
        peerSet = result != 0;

        // channel parameter set ?
        testFlag = 1;
        testFlag = testFlag << CHANNEL_BIT_POSITION;
        result = flag & testFlag;
        channelSet = result != 0;

        // era parameter set ?
        testFlag = 1;
        testFlag = testFlag << ERA_BIT_POSITION;
        result = flag & testFlag;
        eraSet = result != 0;

        // source peer parameter set ?
        testFlag = 1;
        testFlag = testFlag << SOURCE_PEER_BIT_POSITION;
        result = flag & testFlag;
        sourcePeerSet = result != 0;

        // era from parameter set ?
        testFlag = 1;
        testFlag = testFlag << ERA_FROM_BIT_POSITION;
        result = flag & testFlag;
        eraFrom = result != 0;

        // era from parameter set ?
        testFlag = 1;
        testFlag = testFlag << ERA_TO_BIT_POSITION;
        result = flag & testFlag;
        eraTo = result != 0;

        // recipient peer parameter set ?
        testFlag = 1;
        testFlag = testFlag << RECIPIENT_PEER_BIT_POSITION;
        result = flag & testFlag;
        recipientPeerSet = result != 0;

        // offsets parameter set ?
        testFlag = 1;
        testFlag = testFlag << OFFSETS_BIT_POSITION;
        result = flag & testFlag;
        offsetsSet = result != 0;
    }

    @Override
    public boolean peerSet() { return this.peerSet; }

    @Override
    public boolean channelSet() { return this.channelSet; }

    @Override
    public boolean eraSet() { return this.eraSet; }

    public boolean sourcePeerSet() { return this.sourcePeerSet; }

    public boolean eraFromSet() { return this.eraFrom; }

    public boolean eraToSet() { return this.eraTo; }

    public boolean recipientPeerSet() { return this.recipientPeerSet; }

    public boolean offsetsSet() { return this.offsetsSet; }

    @Override
    public String getPeer() { return this.peer; }

    @Override
    public String getFormat() { return this.format; }

    @Override
    public String getChannel() { return this.channel; }

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

    protected byte readByteParameter(InputStream is) throws IOException {
        return PDU_Impl.readByte(is);
    }

    static byte readByte(InputStream is) throws IOException {
        int value = is.read();
        if(value < 0) {
            is.close();
            throw new IOException("read -1: no more data in stream, closed stream");
        }
        return (byte) value;
    }

    protected short readShortParameter(InputStream is) throws IOException {
        int value = this.readByteParameter(is);
        value = value << 8;
        int right = this.readByteParameter(is);
        value += right;
        return (short) value;
    }

    protected int readIntegerParameter(InputStream is) throws IOException {
        int value = this.readShortParameter(is);
        value = value << 16;
        int right = this.readShortParameter(is);
        value += right;
        return value;
    }

    protected long readLongParameter(InputStream is) throws IOException {
        long value = this.readIntegerParameter(is);
        value = value << 32;
        long right = this.readIntegerParameter(is);
        value += right;
        return value;
    }

    protected String readCharSequenceParameter(InputStream is) throws IOException {
        int length = this.readIntegerParameter(is);
        byte[] parameterBytes = new byte[length];

        is.read(parameterBytes);

        return new String(parameterBytes);
    }


    protected void readPeer(InputStream is) throws IOException {
        this.peer = this.readCharSequenceParameter(is);
    }


    protected void readFormat(InputStream is) throws IOException {
        this.format = this.readCharSequenceParameter(is);
    }


    protected void readChannel(InputStream is) throws IOException {
        this.channel = this.readCharSequenceParameter(is);
    }

    protected void readEra(InputStream is) throws IOException {
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
        if(parameter > -1) {
            int newFlag = 1;
            newFlag = newFlag << bit_position;

            return flags | newFlag;
        }

        return flags;
    }

    static void checkValidStream(OutputStream os) throws ASAPException {
        if(os == null) throw new ASAPException("outputstream must not be null");
    }

    static void checkValidSign(CharSequence peer, boolean signed) throws ASAPException {
        if(peer == null || signed) throw new ASAPException("cannot sign with peer == null");
    }

    static void checkValidFormat(CharSequence format) throws ASAPException {
        if(format == null) throw new ASAPException("format must not be null");
    }

    static void checkValidEra(int era) throws ASAPException {
        if(era < -1) throw new ASAPException("era cannot be smaller than -1");
        int maxEra = 2^8-1;
        if(era > maxEra) throw new ASAPException("era exceeded max limit of 2^8-1");
    }

}
