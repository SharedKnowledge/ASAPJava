package net.sharksystem.asap;

import net.sharksystem.asap.protocol.ASAPConnection;
import net.sharksystem.asap.protocol.ASAPOnlineMessageSource;
import net.sharksystem.asap.protocol.ASAP_1_0;
import net.sharksystem.asap.protocol.ASAP_Modem_Impl;
import net.sharksystem.asap.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ASAPSingleProcessOnlineMessageSender
        extends ASAPAbstractOnlineMessageSender implements ASAPOnlineMessageSource {

    private final ASAP_1_0 protocol = new ASAP_Modem_Impl();
    private final MultiASAPEngineFS multiEngine;
    private final ASAPStorage source;
    // connections and their remote peer (recipients)
    private Map<ASAPConnection, CharSequence> connectionPeers = new HashMap<>();

    // message for recipients
    private Map<CharSequence, List<byte[]>> messages = new HashMap<>();

    public ASAPSingleProcessOnlineMessageSender(MultiASAPEngineFS multiEngine, ASAPStorage source) {
        this.source = source;
        this.attachToSource(source);
        this.multiEngine = multiEngine;
    }

    @Override
    public void sendASAPAssimilate(CharSequence format, CharSequence uri, List<CharSequence> recipients,
                                   byte[] messageAsBytes, int era) throws IOException, ASAPException {

        StringBuilder sb = Log.startLog(this);
        sb.append("messageAdded(format: ");
        sb.append(format);
        sb.append(", uri: ");
        sb.append(uri);
        sb.append(", era: ");
        sb.append(era);
        sb.append(", #recipients: ");
        sb.append(recipients.size());
        sb.append(", messageBytes: ");
        sb.append(new String(messageAsBytes));
        sb.append(")");
        System.out.println(sb.toString());

        // each message can have multiple recipients. Iterate

        // is there an open connection to each of the recipients.
        boolean foundAll = true; // optimism captain :)
        for(CharSequence recipient : recipients) {
            sb = Log.startLog(this);
            sb.append("try to find connection for recipient: ");
            sb.append(recipient);
            System.out.println(sb.toString());
            if(multiEngine.existASAPConnection(recipient)) {
                ASAPConnection asapConnection = multiEngine.getASAPConnection(recipient);
                sb = Log.startLog(this);
                sb.append("got asap connection, subscribe / and store message");
                System.out.println(sb.toString());

                // subscribe and remember it
                asapConnection.addOnlineMessageSource(this);
                this.connectionPeers.put(asapConnection, recipient);

                // serialize message for this recipient
                ByteArrayOutputStream asapPDUBytes = new ByteArrayOutputStream();
                protocol.assimilate(this.multiEngine.getOwner(), recipient, format, uri, era, null, // no offsets
                        messageAsBytes, asapPDUBytes, asapConnection.isSigned());

                // I guess maps are synchronized
                List<byte[]> messageList = this.messages.get(recipient);
                if(messageList == null) {
                    messageList = new ArrayList<>();
                    this.messages.put(recipient, messageList);
                }

                messageList.add(asapPDUBytes.toByteArray());

            } else {
                sb = Log.startLog(this);
                sb.append("no connection found");
                System.out.println(sb.toString());
                foundAll = false; // at least to one recipient is not open line
            }
        }
    }

    private String getLogStart() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void sendMessages(ASAPConnection asapConnection, OutputStream os) throws IOException {
        CharSequence recipient = this.connectionPeers.get(asapConnection);

        List<byte[]> messageList = this.messages.get(recipient);
        System.out.println(this.getLogStart() + " send message(s) to " + recipient);
        while(!messageList.isEmpty()) {
            os.write(messageList.remove(0));
        }

        this.messages.remove(recipient);
        asapConnection.removeOnlineMessageSource(this);
        this.connectionPeers.remove(asapConnection);
    }
}
