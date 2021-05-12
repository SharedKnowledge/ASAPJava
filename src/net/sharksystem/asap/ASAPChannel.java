package net.sharksystem.asap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public interface ASAPChannel {
    CharSequence getOwner() throws IOException;
    CharSequence getUri() throws IOException;
    Set<CharSequence> getRecipients() throws IOException;
    HashMap<String, String> getExtraData() throws IOException;
    void putExtraData(String key, String value) throws IOException;
    void removeExtraData(String key) throws IOException;

    /**
     * Get all message in this channel, including received messages. Same as getMessages(true);
     * @return
     * @throws IOException
     */
    ASAPMessages getMessages() throws IOException;

    /**
     * Get messages in this channel only (most probably sent) from this peer or also received messages
     * @param peerOnly
     * @return
     * @throws IOException
     */
    ASAPMessages getMessages(boolean peerOnly) throws IOException, ASAPException;

    /**
     * Get all message (including received messages). Object helps to bring messages in order. Note: It is expected
     * that messages in a previous era do always proceed message in later eras
     *
     * @param compare
     * @return
     * @throws IOException
     */
    ASAPMessages getMessages(ASAPMessageCompare compare) throws IOException, ASAPException;

    /**
     * Add a message to this channel - in other words: broadcast a message into this channel.
     * @param message
     * @throws IOException
     */
    void addMessage(byte[] message) throws IOException;
}
