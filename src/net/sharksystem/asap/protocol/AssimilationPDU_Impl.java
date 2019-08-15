package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

class AssimilationPDU_Impl extends PDU_Impl implements ASAP_AssimilationPDU_1_0 {
    private final long dataLength;
    private final InputStream is;
    private String recipientPeer;
    public static final String OFFSET_DELIMITER = ",";
    private List<Integer> offsets;

    // PDU: CMD | FLAGS | PEER | RECIPIENT | FORMAT | CHANNEL | ERA | OFFSETS | LENGTH | DATA

    public AssimilationPDU_Impl(int flagsInt, InputStream is) throws IOException, ASAPException {
        super(ASAP_1_0.ASSIMILATE_CMD);

        evaluateFlags(flagsInt);

        if(this.peerSet()) { this.readPeer(is); }
        if(this.recipientPeerSet()) { this.readRecipientPeer(is); }
        this.readFormat(is);
        if(this.channelSet()) { this.readChannel(is); }
        if(this.eraSet()) { this.readEra(is); }
        if(this.offsetsSet()) { this.readOffsets(is); }

        this.dataLength = this.readLongParameter(is);

        this.is = is;
    }

    private void readOffsets(InputStream is) throws IOException, ASAPException {
        this.offsets = string2list(this.readCharSequenceParameter(is));
    }

    private void readRecipientPeer(InputStream is) throws IOException {
        this.recipientPeer = this.readCharSequenceParameter(is);
    }

    static void sendPDU(CharSequence peer, CharSequence recipientPeer, CharSequence format, CharSequence channel,
                        int era, long length, List<Long> offsets, InputStream is, OutputStream os, boolean signed)
            throws IOException, ASAPException {

        // first: check protocol errors
        PDU_Impl.checkValidEra(era);
        PDU_Impl.checkValidFormat(format);
        PDU_Impl.checkValidSign(peer, signed);
        PDU_Impl.checkValidStream(os);

        // create parameter bytes
        int flags = 0;
        flags = PDU_Impl.setFlag(peer, flags, PEER_BIT_POSITION);
        flags = PDU_Impl.setFlag(recipientPeer, flags, RECIPIENT_PEER_BIT_POSITION);
        flags = PDU_Impl.setFlag(channel, flags, CHANNEL_BIT_POSITION);
        flags = PDU_Impl.setFlag(era, flags, ERA_BIT_POSITION);
        flags = PDU_Impl.setFlag(offsets, flags, OFFSETS_BIT_POSITION);

        PDU_Impl.sendHeader(ASAP_1_0.ASSIMILATE_CMD, flags, os);

        PDU_Impl.sendCharSequenceParameter(peer, os); // opt
        PDU_Impl.sendCharSequenceParameter(recipientPeer, os); // opt
        PDU_Impl.sendCharSequenceParameter(format, os); // mand
        PDU_Impl.sendCharSequenceParameter(channel, os); // opt
        PDU_Impl.sendIntegerParameter(era, os); // opt
        PDU_Impl.sendCharSequenceParameter(list2string(offsets), os); // opt

        PDU_Impl.sendLongParameter(length, os); // mand

        // stream data
        while(length-- > 0) {
            os.write(is.read());
        }

        // TODO: signature
    }

    static String list2string(List<Long> list) {
        if(list == null || list.size() == 0) return null;

        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for(Long i : list) {
            if(!first) {
                sb.append(OFFSET_DELIMITER);
            }
            else first = false;

            sb.append(i);
        }

        return  sb.toString();
    }

    private List<Integer> string2list(String s) throws ASAPException {
        List<Integer> l = new ArrayList<>();

        StringTokenizer st = new StringTokenizer(s, OFFSET_DELIMITER);

        try {
            while (st.hasMoreTokens()) {
                l.add(Integer.parseInt(st.nextToken()));
            }
        }
        catch(RuntimeException re) {
            throw new ASAPException("malformed offset parameter in received data: " + s);
        }

        return l;

    }

    @Override
    public String getRecipientPeer() { return this.recipientPeer;  }

    @Override
    public long getLength() { return this.dataLength; }

    @Override
    public List<Integer> getMessageOffsets() {
        return this.offsets;
    }

    @Override
    public byte[] getData() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.streamData(baos, this.dataLength);

        return baos.toByteArray();
    }

    @Override
    public void streamData(OutputStream os, long length) throws IOException {
        for(int i = 0; i < length; i++) {
            os.write(this.is.read());
        }
    }
}
