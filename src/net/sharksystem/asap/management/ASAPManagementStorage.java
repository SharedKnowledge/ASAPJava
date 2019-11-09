package net.sharksystem.asap.management;

import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.util.Collection;

public interface ASAPManagementStorage {
    CharSequence ASAP_CREATE_CHANNEL = "asap://createChannel";

    void notifyChannelCreated(CharSequence appName, CharSequence channelUri,
                              CharSequence uri, Collection<CharSequence> storageRecipients) throws ASAPException, IOException;
}
