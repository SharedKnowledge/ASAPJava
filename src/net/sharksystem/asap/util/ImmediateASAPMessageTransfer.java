package net.sharksystem.asap.util;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessageAddListener;
import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.asap.MultiASAPEngineFS;
import net.sharksystem.asap.protocol.ASAPConnection;

import java.io.IOException;
import java.util.List;

public class ImmediateASAPMessageTransfer implements ASAPMessageAddListener {
    private final MultiASAPEngineFS multiEngine;
    private final ASAPStorage source;

    public ImmediateASAPMessageTransfer(MultiASAPEngineFS multiEngine, ASAPStorage source) {
        this.multiEngine = multiEngine;
        this.source = source;
        source.attachASAPMessageAddListener(this);
    }

    public void detachFromStorage() {
        this.source.detachASAPMessageAddListener(this);
    }

    @Override
    public void messageAdded(CharSequence format, CharSequence uri, List<CharSequence> recipients,
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
                sb.append("got asap connection, going to call addMessage()");
                System.out.println(sb.toString());

                asapConnection.addMessage(recipient, uri, format, messageAsBytes, era);

            } else {
                sb = Log.startLog(this);
                sb.append("no connection found");
                System.out.println(sb.toString());
                foundAll = false; // at least to one recipient is not open line
            }
        }

        if(foundAll) {
            sb = Log.startLog(this);
            sb.append("found all recipients for this chunk - should inform asap storage");
            System.out.println(sb.toString());
        }
    }

    private String getLogStart() {
        return this.getClass().getSimpleName();
    }
}
