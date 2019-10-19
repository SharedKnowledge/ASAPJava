package net.sharksystem.asap.protocol;

import java.util.List;

public interface ASAPManagementCreateASAPStoragePDU {
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

    /**
     *
     * @return format/app in which this storage/channel is hosted
     */
    CharSequence getFormat();
}
