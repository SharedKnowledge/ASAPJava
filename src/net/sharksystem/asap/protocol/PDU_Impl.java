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

    protected void evaluateFlags(int flagsInt) {
        // peer parameter set ?
        int flag = 1;
        flag = flag << PEER_BIT_POSITION;
        int result = flagsInt | flag;
        peerSet = result != 0;

        // channel parameter set ?
        flag = 1;
        flag = flag << CHANNEL_BIT_POSITION;
        result = flagsInt | flag;
        channelSet = result != 0;

        // era parameter set ?
        flag = 1;
        flag = flag << ERA_BIT_POSITION;
        result = flagsInt | flag;
        eraSet = result != 0;

        // source peer parameter set ?
        flag = 1;
        flag = flag << SOURCE_PEER_BIT_POSITION;
        result = flagsInt | flag;
        sourcePeerSet = result != 0;

        // era from parameter set ?
        flag = 1;
        flag = flag << ERA_FROM_BIT_POSITION;
        result = flagsInt | flag;
        eraFrom = result != 0;

        // era from parameter set ?
        flag = 1;
        flag = flag << ERA_TO_BIT_POSITION;
        result = flagsInt | flag;
        eraTo = result != 0;

        // recipient peer parameter set ?
        flag = 1;
        flag = flag << RECIPIENT_PEER_BIT_POSITION;
        result = flagsInt | flag;
        recipientPeerSet = result != 0;

        // offsets parameter set ?
        flag = 1;
        flag = flag << OFFSETS_BIT_POSITION;
        result = flagsInt | flag;
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

    static protected void sendCharSequenceParameter(CharSequence parameter, OutputStream os) throws IOException {
        if(parameter == null || parameter.length() < 1) return;
        byte[] bytes = parameter.toString().getBytes();
        os.write(bytes.length);
        os.write(bytes);
    }

    static void sendIntegerParameter(int parameter, OutputStream os) throws IOException {
        os.write(parameter);
    }

    protected String readCharSequenceParameter(InputStream is) throws IOException {
        int length = is.read();
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

    protected int readIntegerParameter(InputStream is) throws IOException {
        int parameter = is.read();
        return parameter;
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

    static int setFlag(List<Integer> parameter, int flags, int bit_position) {
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
