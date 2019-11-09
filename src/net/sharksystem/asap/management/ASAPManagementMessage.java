package net.sharksystem.asap.management;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.protocol.ASAP_1_0;
import net.sharksystem.asap.protocol.ASAP_PDU_1_0;

import java.io.*;
import java.util.*;

public class ASAPManagementMessage {
    public static byte[] getCreateClosedASAPChannelMessage(
            CharSequence owner, CharSequence appName, CharSequence channelUri,
            Collection<CharSequence> recipients) throws ASAPException, IOException {

        if(recipients == null || recipients.size() < 1) {
            throw new ASAPException("recipients in storage/channelUri must not be null or empty: ");
        }

        // we have to put format and recipients into an assimilate message.

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // put owner
        dos.writeUTF(owner.toString());

        // put appName uri
        dos.writeUTF(appName.toString());

        // put channelUri uri
        dos.writeUTF(channelUri.toString());

        // put recipients
        for(CharSequence recipient : recipients) {
            dos.writeUTF(recipient.toString());
        }

        return baos.toByteArray();
    }

    public static ASAPManagementCreateASAPStorageMessage parseASAPManagementMessage(byte[] message)
            throws IOException {

        return new CreateASAPStorageMessage(message);
    }

    private static boolean isASAPManagementMessage(ASAP_PDU_1_0 asapPDU) {
        return asapPDU.getFormat().equalsIgnoreCase(ASAP_1_0.ASAP_MANAGEMENT_FORMAT);
    }

    private static class CreateASAPStorageMessage implements ASAPManagementCreateASAPStorageMessage {
        private final Set<CharSequence> recipients;
        private final CharSequence channelUri;
        private final CharSequence appName;
        private final CharSequence owner;

        CreateASAPStorageMessage(byte[] message) throws IOException {

            // convert to string
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(message));
            this.owner = dis.readUTF();
            this.appName = dis.readUTF();
            this.channelUri = dis.readUTF();

            // there must be at least one recipient
            this.recipients = new HashSet<>();
            this.recipients.add(dis.readUTF());

            // and maybe some more
            try {
                for(;;) {
                    this.recipients.add(dis.readUTF());
                }
            }
            catch(Throwable t) {
                // reach end - ok
            }
        }

        @Override
        public Set<CharSequence> getRecipients() {
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
        public CharSequence getAppName() {
            return this.appName;
        }
    }
}
