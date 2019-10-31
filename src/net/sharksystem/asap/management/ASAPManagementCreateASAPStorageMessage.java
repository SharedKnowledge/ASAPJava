package net.sharksystem.asap.management;

import java.util.List;

public interface ASAPManagementCreateASAPStorageMessage {
    /**
     * @return list of recipients of this storage/channel
     */
    List<CharSequence> getRecipients();

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
