package net.sharksystem.asap;

import net.sharksystem.asap.protocol.*;
import net.sharksystem.asap.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public abstract class ASAPAbstractOnlineMessageSender implements ASAPOnlineMessageSender {
    private ASAPStorage source = null;

    public void attachToSource(ASAPStorage source) {
        source.attachASAPMessageAddListener(this);
    }

    public void detachFromStorage() {
        if(this.source != null) {
            this.source.detachASAPMessageAddListener();
        }
    }

    public void sendASAPAssimilate(CharSequence format, CharSequence uri, CharSequence recipient,
                                            byte[] messageAsBytes, int era) throws IOException, ASAPException {
        if(recipient == null) {
            this.sendASAPAssimilate(format, uri, messageAsBytes, era);
        } else {
            Set<CharSequence> recipients = new HashSet<>();
            recipients.add(recipient);
            this.sendASAPAssimilate(format, uri, recipients, messageAsBytes, era);
        }
    }
}
