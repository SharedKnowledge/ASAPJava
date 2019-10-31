package net.sharksystem.asap.management;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.util.Set;

public interface ASAPManagementStorage {
    CharSequence ASAP_CREATE_CHANNEL = "asap://createChannel";

    void notifyChannelCreated(CharSequence appName, CharSequence channelUri,
                              CharSequence uri, Set<CharSequence> storageRecipients) throws ASAPException, IOException;
}
