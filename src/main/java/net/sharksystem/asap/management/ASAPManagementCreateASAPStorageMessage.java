package net.sharksystem.asap.management;

import java.util.Set;

public interface ASAPManagementCreateASAPStorageMessage {
    /**
     * @return list of recipients of this storage/channel
     */
    Set<CharSequence> getRecipients();

    /**
     * @return channel uri
     */
    CharSequence getChannelUri();

    /**
     *
     * @return storage/channel owner
     */
    CharSequence getOwner();

    CharSequence getAppName();
}
