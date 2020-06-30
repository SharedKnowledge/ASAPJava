package net.sharksystem.asap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public interface ASAPChannel {
    CharSequence getOwner() throws IOException;
    CharSequence getUri() throws IOException;
    Set<CharSequence> getRecipients() throws IOException;
    HashMap<String, String> getExtraData() throws IOException;
    ASAPMessages getMessages() throws IOException;
    void addMessage(byte[] message) throws IOException;
}
