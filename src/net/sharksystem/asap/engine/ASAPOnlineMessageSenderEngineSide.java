package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.protocol.ASAPConnection;
import net.sharksystem.asap.protocol.ASAPOnlineMessageSource;
import net.sharksystem.asap.protocol.ASAP_1_0;
import net.sharksystem.asap.protocol.ASAP_Modem_Impl;
import net.sharksystem.utils.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class ASAPOnlineMessageSenderEngineSide extends ASAPAbstractOnlineMessageSender
        implements ASAPOnlineMessageSource, ASAPOnlineMessageSender {

    private final ASAPInternalPeer multiEngine;
    private final ASAP_1_0 protocol = new ASAP_Modem_Impl();

    // connections and their remote peer (recipients)
    private Map<ASAPConnection, CharSequence> connectionPeers = new HashMap<>();

    // message for recipients
    private Map<CharSequence, List<byte[]>> messages = new HashMap<>();

    public ASAPOnlineMessageSenderEngineSide(ASAPInternalPeer multiEngine) {
        this.multiEngine = multiEngine;
    }

    public void sendASAPAssimilateMessage(CharSequence format, CharSequence uri, byte[] messageAsBytes)
            throws IOException, ASAPException {
        this.sendASAPAssimilateMessage(format, uri, messageAsBytes, ASAPEngineFS.DEFAULT_INIT_ERA);
    }

    public void sendASAPAssimilateMessage(CharSequence format, CharSequence uri, byte[] messageAsBytes, int era)
            throws IOException, ASAPException {

        Set<CharSequence> onlinePeers = this.multiEngine.getOnlinePeers();
        if(onlinePeers == null || onlinePeers.size() < 1) {
            System.out.println(this.getLogStart() + "no online peers");
            throw new ASAPException("no online peers");
        }

        Set<CharSequence> onlinePeerList = new HashSet<>();
        for(CharSequence peerName : onlinePeers) {
            onlinePeerList.add(peerName);
            System.out.println(this.getLogStart() + peerName  + " is online");
        }

        this.sendASAPAssimilateMessage(format, uri, onlinePeerList, messageAsBytes, era);
    }

    public void sendASAPAssimilateMessage(CharSequence format, CharSequence uri, Set<CharSequence> receiver,
                                          byte[] messageAsBytes, int era) throws IOException, ASAPException {

        if(receiver == null || receiver.size() < 1) {
            // replace empty recipient list with list of online peers.
            this.sendASAPAssimilateMessage(format, uri, messageAsBytes, era);
            return;
        }

        StringBuilder sb = Log.startLog(this);
        sb.append("sendASAPAssimilate(format: ");
        sb.append(format);
        sb.append("| uri: ");
        sb.append(uri);
        sb.append("| era: ");
        sb.append(era);
        sb.append("| #receiver: ");
        if(receiver != null) sb.append(receiver.size());
        else sb.append("null");
        sb.append("| length: ");
        sb.append(messageAsBytes.length);
        sb.append(")");
        System.out.println(sb.toString());

        // each message can have multiple receiver. Iterate

        // is there an open connection to each of the receiver.
        boolean foundAll = true; // optimism captain :)
        for(CharSequence recipient : receiver) {
            sb = Log.startLog(this);
            sb.append("try to find connection for recipient: ");
            sb.append(recipient);
            System.out.println(sb.toString());
            if(multiEngine.existASAPConnection(recipient)) {
                ASAPConnection asapConnection = multiEngine.getASAPConnection(recipient);
                sb = Log.startLog(this);
                sb.append("got asap connection, subscribe / and store message");
                System.out.println(sb.toString());

                // serialize message for this recipient
                ByteArrayOutputStream asapPDUBytes = new ByteArrayOutputStream();
                protocol.assimilate(this.multiEngine.getOwner(), recipient, format, uri, era, null, // no offsets
                        null, messageAsBytes, asapPDUBytes, asapConnection.isSigned());

                // I guess maps are synchronized
                List<byte[]> messageList = this.messages.get(recipient);
                if(messageList == null) {
                    messageList = new ArrayList<>();
                    this.messages.put(recipient, messageList);
                }

                messageList.add(asapPDUBytes.toByteArray());

                // subscribe and remember it
                asapConnection.addOnlineMessageSource(this);
                this.connectionPeers.put(asapConnection, recipient);

            } else {
                sb = Log.startLog(this);
                sb.append("no connection found");
                System.out.println(sb.toString());
                foundAll = false; // at least to one recipient is not open line
            }
        }
    }

    private String getLogStart() {
        return this.getClass().getSimpleName() + ": ";
    }

    @Override
    public void sendStoredMessages(ASAPConnection asapConnection, OutputStream os) throws IOException {
        CharSequence recipient = this.connectionPeers.get(asapConnection);

        List<byte[]> messageList = this.messages.get(recipient);
        System.out.println(this.getLogStart() + "send message(s) to " + recipient);
        System.out.println(this.getLogStart() + "send message to " + recipient);
        System.out.println(this.getLogStart() + "outstream: " + os.getClass().getSimpleName());
        while(!messageList.isEmpty()) {
            byte[] messageBytes = messageList.remove(0);
            os.write(messageBytes);
            System.out.println(this.getLogStart() + "wrote pure bytes: " + messageBytes.length);
        }

        this.messages.remove(recipient);
        asapConnection.removeOnlineMessageSource(this);
        this.connectionPeers.remove(asapConnection);
    }
}
