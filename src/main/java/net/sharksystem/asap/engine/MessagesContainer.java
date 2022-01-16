package net.sharksystem.asap.engine;

import net.sharksystem.asap.ASAPHop;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface MessagesContainer {
    void addMessage(InputStream is, long length) throws IOException;
    void setASAPHopList(List<ASAPHop> asapHopList) throws IOException;
}
