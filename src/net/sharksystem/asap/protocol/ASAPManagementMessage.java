package net.sharksystem.asap.protocol;

import net.sharksystem.asap.ASAPException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ASAPManagementMessage {
    public static byte[] getCreateClosedASAPChannelMessage(
            CharSequence owner, CharSequence appName, CharSequence channelUri,
            List<CharSequence> recipients) throws ASAPException, IOException {

        if(recipients == null || recipients.size() < 1) {
            throw new ASAPException("recipients in storage/channelUri must not be null or empty: ");
        }

        // we have to put format and recipients into an assimilate message.

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // put appName uri second
        dos.writeUTF(appName.toString());

        // put channelUri uri second
        dos.writeUTF(channelUri.toString());

        // put recipients
        for(CharSequence recipient : recipients) {
            dos.writeUTF(recipient.toString());
        }

        return baos.toByteArray();
    }

    public static ASAPManagementCreateASAPStorageMessage parseASAPManagementMessage(CharSequence owner, byte[] message)
            throws IOException {

        return new CreateASAPStorageMessage(owner, message);
    }

    private static class CreateASAPStorageMessage implements ASAPManagementCreateASAPStorageMessage {
        private final List<CharSequence> recipients;
        private final CharSequence channelUri;
        private final CharSequence appName;
        private final CharSequence owner;

        CreateASAPStorageMessage(CharSequence owner, byte[] message) throws IOException {
            this.owner = owner;

            // convert to string
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(message));
            this.appName = dis.readUTF();
            this.channelUri = dis.readUTF();

            // there must be at least one recipient
            this.recipients = new ArrayList<>();
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
        public CharSequence getAppName() {
            return this.appName;
        }
    }
}
