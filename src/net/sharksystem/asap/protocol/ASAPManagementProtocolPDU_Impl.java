package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static net.sharksystem.asap.protocol.ASAP_1_0.ERA_NOT_DEFINED;

public class ASAPManagementProtocolPDU_Impl {
    public static void sendCreateASAPStoragePDU(
            CharSequence owner, List<CharSequence> storageRecipients,
            CharSequence format, CharSequence channel,
            OutputStream os, boolean signed) throws IOException, ASAPException {

        if(format == null || format.length() < 1) {
            throw new ASAPException("format must not be null or empty");
        }

        if(storageRecipients == null || storageRecipients.size() < 1) {
            throw new ASAPException("recipients in storage/channel must not be null or empty: " + format);
        }

        // we have to put format and recipients into an assimilate message.

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // put format first
        dos.writeUTF(format.toString());

        // remember following offset
        List<Long> offsets = new ArrayList<>();

        for(CharSequence recipient : storageRecipients) {
            // remember offset
            offsets.add((long) baos.size());
            // write recipient
            dos.writeUTF(recipient.toString());
        }

        // we have compiled the messageBytes
        byte[] messageBytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(messageBytes);

        AssimilationPDU_Impl.sendPDU(
                owner, // storage owner is also sender
                null,
                ASAP_1_0.ASAP_MANAGEMENT_FORMAT, //
                channel, // channel that shall be created
                ERA_NOT_DEFINED,
                messageBytes.length,
                offsets,
                bais,
                os,
                signed);
    }

    public static ASAPManagementCreateASAPStoragePDU parseASAPPDU(ASAP_AssimilationPDU_1_0 pdu)
            throws IOException {

        return new CreateASAPStoragePDU(pdu);
    }

    private static class CreateASAPStoragePDU implements ASAPManagementCreateASAPStoragePDU {
        private final List<CharSequence> recipients;
        private final CharSequence channelUri;
        private final CharSequence owner;
        private final CharSequence format;

        CreateASAPStoragePDU(ASAP_AssimilationPDU_1_0 pdu) throws IOException {
            this.owner = pdu.getPeer(); // sender is owner

            // channel in pdu
            if(pdu.channelSet()) {
                this.channelUri = pdu.getChannel();
            } else {
                this.channelUri = null;
            }

            // format and recipients are stored in content
            List<Integer> offsets = pdu.getMessageOffsets();
            InputStream is = pdu.getInputStream();

            // read format
            byte[] stringAsBytes = new byte[offsets.remove(0)];
            is.read(stringAsBytes);

            // convert to string
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(stringAsBytes));
            this.format = dis.readUTF();

            // read recipients
            this.recipients = new ArrayList<>();
            for(int offset : offsets) {
                dis = new DataInputStream(new ByteArrayInputStream(new byte[offset]));
                this.recipients.add(dis.readUTF());
            }
        }

        @Override
        public List<CharSequence> getRecipients() {
            return this.recipients;
        }

        @Override
        public CharSequence getChannelUri() {
            return this.channelUri;
        }

        @Override
        public CharSequence getOwner() {
            return this.owner;
        }

        @Override
        public CharSequence getFormat() {
            return this.format;
        }
    }
}
