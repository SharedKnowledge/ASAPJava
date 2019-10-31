package net.sharksystem.asap.management;

import net.sharksystem.asap.*;
import net.sharksystem.asap.protocol.ASAP_1_0;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ASAPManagementMessageHandler implements ASAPChunkReceivedListener {
    private final MultiASAPEngineFS multiASAPEngine;

    public ASAPManagementMessageHandler(MultiASAPEngineFS multiASAPEngine) throws IOException, ASAPException {
        this.multiASAPEngine = multiASAPEngine;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    //                            chunk received listener for asap management engine                     //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void chunkReceived(String sender, String uri, int era) {
        System.out.println(this.getLogStart()
                + "handle received chunk (sender/uri/era)" + sender + "/" + uri + "/" + era);
        try {
            ASAPEngine asapManagementEngine = multiASAPEngine.getEngineByFormat(ASAP_1_0.ASAP_MANAGEMENT_FORMAT);
            CharSequence owner = asapManagementEngine.getOwner();

            ASAPChunkStorage incomingChunkStorage = asapManagementEngine.getIncomingChunkStorage(sender);
            ASAPChunk chunk = incomingChunkStorage.getChunk(uri, era);
            Iterator<byte[]> messageIter = chunk.getMessagesAsBytes();
            System.out.println(this.getLogStart() + "iterate management messages");
            while(messageIter.hasNext()) {
                byte[] message = messageIter.next();
                this.handleASAPManagementMessage(message);

                // add message without changes - could be signed
                asapManagementEngine.add(uri, message);
            }
            System.out.println(this.getLogStart() + "done iterating management messages");
            // remove incoming messages - handled
            asapManagementEngine.getIncomingChunkStorage(sender).dropChunks(era);
            System.out.println(this.getLogStart() + "incoming asap management messages dropped");
        } catch (ASAPException | IOException e) {
            System.out.println("could get asap management engine but received chunk - looks like a bug");
        }
    }

    private void handleASAPManagementMessage(byte[] message) throws ASAPException, IOException {
        StringBuilder b = new StringBuilder();
        b.append(this.getLogStart());
        b.append("start processing asap management pdu");
        System.out.println(b.toString());

        ASAPManagementCreateASAPStorageMessage asapManagementCreateASAPStorageMessage =
                ASAPManagementMessage.parseASAPManagementMessage(message);

        CharSequence owner = asapManagementCreateASAPStorageMessage.getOwner();
        CharSequence channelUri = asapManagementCreateASAPStorageMessage.getChannelUri();
        CharSequence format = asapManagementCreateASAPStorageMessage.getAppName();
        List<CharSequence> receivedRecipients = asapManagementCreateASAPStorageMessage.getRecipients();

        // add owner to this list
        List<CharSequence> recipients = new ArrayList<>();

        // add owner
        recipients.add(owner);

        // add rest
        for(CharSequence r : receivedRecipients) {
            recipients.add(r);
        }

        b = new StringBuilder();
        b.append(this.getLogStart());
        b.append("owner: ");
        b.append(owner);
        b.append(" | format: ");
        b.append(format);
        b.append(" | channelUri: ");
        b.append(channelUri);
        b.append(" | #recipients: ");
        b.append(recipients.size());

        // find storage / app - can throw an exception - that's ok
        ASAPStorage asapStorage = this.multiASAPEngine.getEngineByFormat(format);

        if(asapStorage.channelExists(channelUri)) {
            Set<CharSequence> existingChannelRecipientsList = asapStorage.getRecipients(channelUri);
            if(existingChannelRecipientsList.size() == recipients.size()) {
                // could be the same - same recipients?

                // iterate all recipient and check whether they are also in local recipient list
                for(CharSequence recipient : recipients) {
                    boolean found = false;
                    for(CharSequence existingRecipient : existingChannelRecipientsList) {
                        if(existingRecipient.toString().equalsIgnoreCase(recipient.toString())) {
                            // got it
                            found = true;
                            break; // leave loop and test next
                        }
                    }
                    if(!found) {
                        throw new ASAPException("channel already exists but with different recipients: " + b.toString());
                    }
                }
                // ok it the same
                return;
            } else {
                throw new ASAPException("channel already exists but with different settings: " + b.toString());
            }
        }

        // else - channel does not exist - create by setting recipients
        System.out.println(this.getLogStart() + "create channel: " + b.toString());
        asapStorage.createChannel(owner, channelUri, recipients);
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + "(" + this.multiASAPEngine.getOwner() + "): ";
    }
}
