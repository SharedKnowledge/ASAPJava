package net.sharksystem.asap;

public interface ASAPChannelContentChangedListenerManagement {
    /**
     * Add listener for changes in ASAP environment
     * @param listener listener which is to be added
     */
    void addASAPChannelContentChangedListener(CharSequence format, ASAPChannelContentChangedListener listener);

    /**
     * Remove changes listener
     * @param listener listener which is to be removed
     */
    void removeASAPChannelContentChangedListener(CharSequence format, ASAPChannelContentChangedListener listener);

}
