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
    }

    @Override
    public void messageAdded(CharSequence format, CharSequence urlTarget, List<CharSequence> recipients,
                             byte[] messageAsBytes, int era) throws IOException, ASAPException {

        StringBuilder sb = Log.startLog(this);
        sb.append("message added to uri == ");
        sb.append(urlTarget);
        sb.append(" | era == ");
        sb.append(era);
        System.out.println(sb.toString());

        // is there an open connection to each of the recipients.
        boolean foundAll = true; // optimism captain :)
        for(CharSequence recipient : recipients) {
            if(multiEngine.existASAPConnection(recipient)) {
                ASAPConnection asapConnection = multiEngine.getASAPConnection(recipient);

                asapConnection.addMessage(recipient, urlTarget, format, messageAsBytes, era);
            } else {
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
