package net.sharksystem.asap;

import java.io.IOException;
import java.util.List;

public interface ASAPManagementStorage {
    CharSequence ASAP_CREATE_CHANNEL = "asap://createChannel";

    void addCreateClosedASAPChannelMessage(CharSequence appName, CharSequence channelUri,
                       List<CharSequence> storageRecipients) throws ASAPException, IOException;
}
