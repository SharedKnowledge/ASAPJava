package net.sharksystem.asap;

import net.sharksystem.asap.protocol.*;
import net.sharksystem.asap.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ASAPAbstractOnlineMessageSender implements ASAPOnlineMessageSender {
    private ASAPStorage source = null;

    public void attachToSource(ASAPStorage source) {
        source.attachASAPMessageAddListener(this);
    }

    public void detachFromStorage() {
        if(this.source != null) {
            this.source.detachASAPMessageAddListener(this);
        }
    }
}
