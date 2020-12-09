package net.sharksystem.asap.apps;

public interface ASAPMessageReceivedListenerManagement {
    /**
     * Add a listener that is called when a message came in
     * @param format app name == supported format
     * @param listener listener object
     */
    void addASAPMessageReceivedListener(CharSequence format, ASAPMessageReceivedListener listener);
    void removeASAPMessageReceivedListener(CharSequence format, ASAPMessageReceivedListener listener);
}
