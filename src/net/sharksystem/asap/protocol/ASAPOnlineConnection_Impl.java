package net.sharksystem.asap.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ASAPOnlineConnection_Impl extends Thread implements ASAPOnlineConnection {
    private final ASAPOnlineConnectionListener asapOnlineConnectionListener;
    private final CharSequence peer;
    private final InputStream is;
    private final OutputStream os;
    private final ThreadFinishedListener threadFinishedListener;

    private List<byte[]> onlineMessageList = new ArrayList<>();
    private List<ASAPOnlineMessageSource> onlineMessageSources = new ArrayList<>();

    public ASAPOnlineConnection_Impl(CharSequence peerID,
                                     InputStream inputStream,
                                     OutputStream outputStream,
                                     ASAPOnlineConnectionListener asapOnlineConnectionListener,
                                     ThreadFinishedListener threadFinishedListener) {

        this.peer = peerID;
        this.is = inputStream;
        this.os = outputStream;
        this.asapOnlineConnectionListener = asapOnlineConnectionListener;
        this.threadFinishedListener = threadFinishedListener;
    }

    @Override
    public CharSequence getRemotePeer() {
        return this.peer;
    }

    @Override
    public void addOnlineMessageSource(ASAPOnlineMessageSource source) {
        this.onlineMessageSources.add(source);
    }

    @Override
    public void removeOnlineMessageSource(ASAPOnlineMessageSource source) {
        this.onlineMessageSources.remove(source);
    }

    public boolean isSigned() {
        return false; // TODO
    }
    private synchronized void addOnlineMessage(byte[] message) {
        this.onlineMessageList.add(message);
    }

    private synchronized byte[] removeOnlineMessage() {
        return this.onlineMessageList.remove(0);
    }


    private boolean sendOnlineMessages() throws IOException {
        boolean sent = false;

        if(!this.onlineMessageSources.isEmpty()) {
            StringBuilder sb = this.getLogStart();
            sb.append("check non empty online message");
            System.out.println(sb.toString());

            for(int i = 0; i < this.onlineMessageSources.size(); i++) {
                ASAPOnlineMessageSource asapOnline = this.onlineMessageSources.get(i);
                sent |= asapOnline.sendMessages(this, this.os);
            }
        }

        return sent;
    }

    public void run() {
        // online loop goes here
        StringBuilder sb = this.getLogStart();
        sb.append("started");
        System.out.println(sb.toString());

/*
            // fresh messages? would be better to run that parallel to read!
            boolean sentOnlineMessage = false;
            try {
                // take this time to send online messages
                System.out.println(this.getLogStart() + "send online...");
                if(this.sendOnlineMessages()) {
                    System.out.println(this.getLogStart() + ".. sent something");
                    sentOnlineMessage = true;
                } else {
                    System.out.println(this.getLogStart() + ".. no online messages to sent");
                }
            } catch (IOException e) {
                this.finishStartup("problems when sending online messages", e);
                return;
            }


 */
    }

    private StringBuilder getLogStart() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append(": ");
        return sb;
    }

}
